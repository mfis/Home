package homecontroller.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import homelibrary.homematic.model.Datapoint;
import homelibrary.homematic.model.Device;
import homelibrary.homematic.model.HomematicCommand;

@Component
public class HomematicAPI {

	@Autowired
	private Environment env;

	@Autowired
	@Qualifier("restTemplateCCU")
	private RestTemplate restTemplateCCU;

	private static final int INIT_STATE_MINUTES = 90;

	private static final DateTimeFormatter UPTIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss");

	private String host;

	private String ccuUser;

	private String ccuPass;

	private boolean writeToHomematicEnabled;

	private static final String REGA_HTTPS_PORT_AND_URI = ":48181/tclrega.exe";

	private LocalDateTime currentValuesTimestamp;

	private Map<HomematicCommand, String> currentValues = new HashMap<>();

	private boolean ccuInitState;

	private String refreshHashString = StringUtils.EMPTY;

	private MessageDigest digest = null;

	private long resourceNotAvailableCounter;

	private long lastAuthenticationTestTimestamp = 0;

	private static final Log LOG = LogFactory.getLog(HomematicAPI.class);

	@PostConstruct
	public void init() {

		ccuUser = env.getProperty("homematic.authuser");
		ccuPass = env.getProperty("homematic.authpass");
		host = env.getProperty("homematic.hostName");
		if (!host.startsWith("https://")) {
			throw new IllegalArgumentException("Non-SSL connections to ccu not allowed!");
		}

		if (!testAuthenticationIsActive()) {
			throw new IllegalArgumentException("Rega API authentication is not active!");
		}

		writeToHomematicEnabled = Boolean
				.parseBoolean(env.getProperty("application.write.to.homematic").trim());

		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LOG.error(e);
		}
	}

	public String getAsString(HomematicCommand command) {
		return checkInit(readValue(command));
	}

	public boolean getAsBoolean(HomematicCommand command) {
		return checkInit(Boolean.valueOf(readValue(command)));
	}

	public BigDecimal getAsBigDecimal(HomematicCommand command) {
		return checkInit(new BigDecimal(readValue(command)));
	}

	public Long getTimestamp(HomematicCommand command) {
		ZonedDateTime zonedDateTime = ZonedDateTime
				.of(LocalDateTime.parse(readValue(command), UPTIME_FORMATTER), ZoneId.systemDefault());
		return checkInit(zonedDateTime.toInstant().toEpochMilli());
	}

	public void executeCommand(HomematicCommand... commands) {
		if (writeToHomematicEnabled) {
			executeCommands(false, commands);
		}
	}

	private String readValue(HomematicCommand command) {
		String key = command.buildVarName();
		if (currentValues.containsKey(command)) {
			return currentValues.get(command);
		} else {
			StringBuilder sb = new StringBuilder();
			for (HomematicCommand cmd : currentValues.keySet()) {
				sb.append(cmd.buildVarName() + "\n");
			}
			LOG.error("Key/Value unknown: " + key + " / known keys: \n" + sb.toString());
			return null;
		}
	}

	private <T> T checkInit(T value) {

		if (!ccuInitState) {
			return value;
		}

		if (value == null) {
			return value;
		}

		if (value.getClass().isAssignableFrom(String.class) && StringUtils.isBlank((String) value)) {
			return null;
		}

		if (value.getClass().isAssignableFrom(BigDecimal.class)
				&& BigDecimal.ZERO.compareTo((BigDecimal) value) == 0) {
			return null;
		}

		return value;
	}

	private Boolean testAuthenticationIsActive() {

		String body = buildReGaRequestBody(HomematicCommand.eof());
		try {
			callReGaAPI(body, false, false);
		} catch (HttpClientErrorException e) {
			if (e.getRawStatusCode() == 401) {
				return true;
			}
			return null; // NOSONAR
		} catch (Exception e) {
			return null; // NOSONAR
		}
		return false;
	}

	public synchronized boolean refresh() {

		List<HomematicCommand> commands = new LinkedList<>();
		for (Device device : Device.values()) {
			for (Datapoint datapoint : device.getDatapoints()) {
				commands.add(HomematicCommand.read(device, datapoint));
				if (datapoint.isTimestamp()) {
					commands.add(HomematicCommand.readTS(device, datapoint));
				}
			}
			if (device.getSysVars() != null) {
				for (String suffix : device.getSysVars()) {
					commands.add(HomematicCommand.read(device, suffix));
				}
			}
			Datapoint lowBatDatapoint = device.lowBatDatapoint();
			if (lowBatDatapoint != null) {
				commands.add(HomematicCommand.read(device, device.lowBatDatapoint()));
			}
		}
		commands.add(HomematicCommand.read("CCU_im_Reboot"));
		commands.add(HomematicCommand.read("CCU_Uptime"));

		boolean refreshed = executeCommands(true, commands.toArray(new HomematicCommand[commands.size()]));
		if (refreshed) {
			lookupInitState();
		}
		return refreshed;
	}

	private void lookupInitState() {

		boolean reboot = getAsBoolean(HomematicCommand.read("CCU_im_Reboot"));
		if (reboot) {
			ccuInitState = true;
			return;
		}

		String uptime = getAsString(HomematicCommand.read("CCU_Uptime"));
		long minutesUptime = Duration
				.between(LocalDateTime.parse(uptime, UPTIME_FORMATTER), LocalDateTime.now()).toMinutes();
		ccuInitState = minutesUptime <= INIT_STATE_MINUTES;
	}

	private boolean executeCommands(boolean refresh, HomematicCommand... commands) {
		String body = buildReGaRequestBody(commands);
		return extractCommandResults(callReGaAPI(body, refresh, true), commands);
	}

	private boolean extractCommandResults(Document responseDocument, HomematicCommand... commands) {

		if (responseDocument == null) {
			return false;
		}

		Map<String, String> newStringToValues = new HashMap<>();
		Map<HomematicCommand, String> newCommandToValues = new HashMap<>();
		boolean rcsOk = true;
		boolean eofOK = false;

		NodeList childs = responseDocument.getElementsByTagName("xml").item(0).getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Element child = (Element) childs.item(i);
			if (child.getTagName().startsWith(HomematicCommand.PREFIX_VAR)) {
				extractVar(newStringToValues, child);
			} else if (child.getTagName().startsWith(HomematicCommand.PREFIX_RC)) {
				rcsOk = extractWriteExecRc(rcsOk, child);
			} else if (child.getTagName().equals(HomematicCommand.E_O_F)) {
				eofOK = extractEof(child);
			}
		}

		if (rcsOk && eofOK) {
			for (HomematicCommand command : commands) {
				newCommandToValues.put(command, newStringToValues.get(command.buildVarName()));
			}
			currentValues = newCommandToValues;
			currentValuesTimestamp = LocalDateTime.now();
		}
		return rcsOk && eofOK;
	}

	private boolean extractEof(Element child) {

		boolean eofOK;
		eofOK = HomematicCommand.E_O_F.equals(child.getTextContent());
		if (!eofOK) {
			LOG.error("Command EOF not OK!");
		}
		return eofOK;
	}

	private boolean extractWriteExecRc(boolean rcsOk, Element child) {

		boolean rc = Boolean.parseBoolean(child.getTextContent());
		rcsOk = rcsOk && rc;
		if (!rc) {
			LOG.error("CommandResult not OK: " + child.getTagName());
		}
		return rcsOk;
	}

	private void extractVar(Map<String, String> newStringToValues, Element child) {

		newStringToValues.put(child.getTagName(), child.getTextContent());
	}

	private String buildReGaRequestBody(HomematicCommand... commands) {

		StringBuilder sb = new StringBuilder(commands.length * 120);
		boolean containsExecuteCommand = false;
		for (HomematicCommand command : commands) {
			if (command.isProgramRunCommand()) {
				containsExecuteCommand = true;
			}
			sb.append(command.buildCommand());
			sb.append(" \n");
		}

		if (containsExecuteCommand) {
			sb.insert(0, HomematicCommand.write("refreshadress", env.getProperty("refresh.adress"))
					.buildCommand());
		}

		sb.append(HomematicCommand.eof().buildCommand());
		return sb.toString();
	}

	HttpHeaders createHeaders(int contentLength, boolean withAuthentication) {

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CACHE_CONTROL, "no-cache");
		if (withAuthentication) {
			String encoding = Base64.getEncoder()
					.encodeToString(ccuUser.concat(":").concat(ccuPass).getBytes(StandardCharsets.UTF_8));
			httpHeaders.set(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
		}
		httpHeaders.setAccept(Arrays.asList(MediaType.ALL));
		httpHeaders.setCacheControl(CacheControl.noCache());
		httpHeaders.setContentLength(contentLength);
		return httpHeaders;
	}

	private Document callReGaAPI(String body, boolean refresh, boolean withAuthentication) {

		HttpHeaders headers = createHeaders(body.length(), withAuthentication);
		HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> responseEntity = null;
		try {
			responseEntity = restTemplateCCU.postForEntity(host + REGA_HTTPS_PORT_AND_URI, requestEntity,
					String.class);
			resourceNotAvailableCounter = 0;
		} catch (Exception e) {
			if (!withAuthentication) {
				throw e;
			}
			resourceNotAvailableCounter++;
			if (resourceNotAvailableCounter > 2) {
				throw e;
			}
			return null;
		}
		HttpStatus statusCode = responseEntity.getStatusCode();
		if (!statusCode.is2xxSuccessful()) {
			LOG.error("Could not successful call ReGa API. RC=" + statusCode.value());
		}

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			byte[] responseBytes = responseEntity.getBody().getBytes(StandardCharsets.UTF_8);
			if (refresh) {
				String actualHashString = Hex.encodeHexString(
						digest.digest(responseEntity.getBody().getBytes(StandardCharsets.UTF_8)));
				if (StringUtils.equals(refreshHashString, actualHashString)) {
					return null;
				} else {
					refreshHashString = actualHashString;
				}
			}
			InputStream inputStream = new ByteArrayInputStream(responseBytes);
			Document doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			throw new IllegalStateException("Error parsing document", e);
		}
	}

	public Map<HomematicCommand, String> getCurrentValues() {
		return currentValues;
	}

	public LocalDateTime getCurrentValuesTimestamp() {
		return currentValuesTimestamp;
	}

}
