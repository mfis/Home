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
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import homelibrary.homematic.model.Datapoint;
import homelibrary.homematic.model.Device;
import homelibrary.homematic.model.HomematicCommand;

@Component
public class HomematicAPI {

	private static final int INIT_STATE_MINUTES = 90;

	private static final DateTimeFormatter UPTIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Autowired
	private Environment env;

	@Autowired
	private RestTemplate restTemplate;

	private String host;

	private boolean writeToHomematicEnabled;

	private static final String REGA_PORT_AND_URI = ":8181/tclrega.exe";

	private LocalDateTime currentValuesTimestamp;

	private Map<HomematicCommand, String> currentValues = new HashMap<>();

	private boolean ccuInitState;

	private String refreshHashString = "";

	private MessageDigest digest = null;

	private static final Log LOG = LogFactory.getLog(HomematicAPI.class);

	@PostConstruct
	public void init() {
		host = env.getProperty("homematic.hostName");
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

	private boolean executeCommands(boolean writeHash, HomematicCommand... commands) {
		String body = buildReGaRequestBody(commands);
		return extractCommandResults(callReGaAPI(body, writeHash), commands);
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
			Node c = childs.item(i);
			Element child = (Element) c;
			if (child.getTagName().startsWith(HomematicCommand.PREFIX_VAR)) {
				newStringToValues.put(child.getTagName(), child.getTextContent());
			} else if (child.getTagName().startsWith(HomematicCommand.PREFIX_RC)) {
				boolean rc = Boolean.parseBoolean(child.getTextContent());
				rcsOk = rcsOk && rc;
				if (!rc) {
					LOG.error("CommandResult not OK: " + child.getTagName());
				}
			} else if (child.getTagName().equals(HomematicCommand.E_O_F)) {
				eofOK = HomematicCommand.E_O_F.equals(child.getTextContent());
				if (!eofOK) {
					LOG.error("Command EOF not OK!");
				}
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

	HttpHeaders createHeaders(int contentLength) {

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Cache-Control", "no-cache");
		httpHeaders.setAccept(Arrays.asList(MediaType.ALL));
		httpHeaders.setCacheControl(CacheControl.noCache());
		httpHeaders.setContentLength(contentLength);
		return httpHeaders;
	}

	private Document callReGaAPI(String body, boolean writeHash) {

		HttpHeaders headers = createHeaders(body.length());
		HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> responseEntity = restTemplate.postForEntity(host + REGA_PORT_AND_URI,
				requestEntity, String.class);
		HttpStatus statusCode = responseEntity.getStatusCode();
		if (!statusCode.is2xxSuccessful()) {
			LOG.error("Could not successful call ReGa API. RC=" + statusCode.value());
		}

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			byte[] responseBytes = responseEntity.getBody().getBytes(StandardCharsets.UTF_8);
			if (writeHash) {
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
