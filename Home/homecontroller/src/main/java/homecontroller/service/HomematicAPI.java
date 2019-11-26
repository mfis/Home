package homecontroller.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

	private static final String REGA_PORT_AND_URI = ":8181/tclrega.exe";

	private Map<String, String> currentValues = new HashMap<>();

	private boolean ccuInitState;

	private static final Log LOG = LogFactory.getLog(HomematicAPI.class);

	@PostConstruct
	public void init() {
		host = env.getProperty("homematic.hostName");
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
		executeCommands(commands);
	}

	private String readValue(HomematicCommand command) {
		String key = command.buildVarName();
		if (currentValues.containsKey(key)) {
			return currentValues.get(key);
		} else {
			LOG.error("Value unknown: " + key);
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

	public void refresh() {

		List<HomematicCommand> commands = new LinkedList<>();
		for (Device device : Device.values()) {
			commands.add(HomematicCommand.read(device, device.lowBatDatapoint()));
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
			commands.add(HomematicCommand.read(device, device.lowBatDatapoint()));
		}
		commands.add(HomematicCommand.read("CCU_im_Reboot"));
		commands.add(HomematicCommand.read("CCU_Uptime"));

		executeCommands(commands.toArray(new HomematicCommand[commands.size()]));

		lookupInitState();
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

	private boolean executeCommands(HomematicCommand... commands) {
		String body = buildReGaRequestBody(commands);
		return extractCommandResults(callReGaAPI(body));
	}

	private boolean extractCommandResults(Document responseDocument) {

		Map<String, String> newValues = new HashMap<>();

		boolean rcsOk = true;
		boolean eofOK = false;

		NodeList childs = responseDocument.getElementsByTagName("xml").item(0).getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node c = childs.item(i);
			Element child = (Element) c;
			if (child.getTagName().startsWith(HomematicCommand.PREFIX_VAR)) {
				newValues.put(child.getTagName(), child.getTextContent());
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
			currentValues = newValues;
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

	private Document callReGaAPI(String body) {

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
			InputStream inputStream = new ByteArrayInputStream(
					responseEntity.getBody().getBytes(StandardCharsets.UTF_8));
			Document doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			throw new IllegalStateException("Error parsing document", e);
		}
	}

}
