package homecontroller.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class HomematicAPI {

	private static final String ID = "id";

	private static final String ISE_ID = "ise_id";

	private static final String NAME = "name";

	private static final String TYPE = "type";

	private static final String VALUE = "value";

	private static final String TIMESTAMP = "timestamp";

	private static final int INIT_STATE_MINUTES = 90;

	private static final DateTimeFormatter UPTIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Autowired
	private Environment env;

	@Autowired
	private RestTemplate restTemplate;

	private String host;

	private Map<String, String> currentValues;
	private Map<String, String> currentStateIDs;
	private Map<String, Long> currentTimestamps;
	private boolean ccuInitState;

	@PostConstruct
	public void init() {
		host = env.getProperty("homematic.hostName");
	}

	public String getAsString(String key) {
		if (currentValues.containsKey(key)) {
			return checkInit(currentValues.get(key));
		} else {
			return null;
		}
	}

	public boolean getAsBoolean(String key) {
		if (currentValues.containsKey(key)) {
			return checkInit(Boolean.valueOf(currentValues.get(key)));
		} else {
			return false;
		}
	}

	public BigDecimal getAsBigDecimal(String key) {
		if (currentValues.containsKey(key)) {
			return checkInit(new BigDecimal(currentValues.get(key)));
		} else {
			return null;
		}
	}

	public Long getTimestamp(String key) {
		if (currentTimestamps.containsKey(key)) {
			return currentTimestamps.get(key);
		} else {
			return null;
		}
	}

	public void changeBooleanState(String key, boolean value) {

		changeString("refreshadress", env.getProperty("refresh.adress"));

		String iseID = currentStateIDs.get(key);
		String url = host + "/addons/xmlapi/statechange.cgi?ise_id=" + iseID + "&new_value="
				+ Boolean.toString(value);
		documentFromUrl(url);
	}

	public synchronized void runProgram(String name) {

		changeString("refreshadress", env.getProperty("refresh.adress"));

		String id = currentStateIDs.get(name);
		String url = host + "/addons/xmlapi/runprogram.cgi?program_id=" + id;
		documentFromUrl(url);
	}

	public void changeString(String key, String value) {

		String iseID = currentStateIDs.get(key);
		String url = host + "/addons/xmlapi/statechange.cgi?ise_id=" + iseID + "&new_value=" + value;
		documentFromUrl(url);
	}

	private <T> T checkInit(T value) {

		if (!ccuInitState) {
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

		currentValues = new HashMap<>();
		currentStateIDs = new HashMap<>();
		currentTimestamps = new HashMap<>();

		Document doc = documentFromUrl(host + "/addons/xmlapi/statelist.cgi");
		NodeList datapoints = doc.getElementsByTagName("datapoint");
		for (int dap = 0; dap < datapoints.getLength(); dap++) {
			Node c = datapoints.item(dap);
			Element eElement = (Element) c;
			if (eElement.getAttribute(VALUE) != null && eElement.getAttribute(VALUE).length() > 0) {
				currentValues.put(eElement.getAttribute(NAME), eElement.getAttribute(VALUE));
			}
			if (eElement.getAttribute(TIMESTAMP) != null && eElement.getAttribute(TIMESTAMP).length() > 1) {
				currentTimestamps.put(eElement.getAttribute(NAME),
						Long.parseLong(eElement.getAttribute(TIMESTAMP)) * 1000L);
			}
			if (eElement.getAttribute(TYPE) != null
					&& eElement.getAttribute(TYPE).equalsIgnoreCase("STATE")) {
				currentStateIDs.put(eElement.getAttribute(NAME), eElement.getAttribute(ISE_ID));
			}
		}

		doc = documentFromUrl(host + "/addons/xmlapi/sysvarlist.cgi");
		NodeList systemVariables = doc.getElementsByTagName("systemVariable");
		for (int dap = 0; dap < systemVariables.getLength(); dap++) {
			Node c = systemVariables.item(dap);
			Element eElement = (Element) c;
			if (eElement.getAttribute(VALUE) != null && eElement.getAttribute(VALUE).length() > 0) {
				currentValues.put(eElement.getAttribute(NAME), eElement.getAttribute(VALUE));
			}
			currentStateIDs.put(eElement.getAttribute(NAME), eElement.getAttribute(ISE_ID));
		}

		doc = documentFromUrl(host + "/addons/xmlapi/programlist.cgi");
		NodeList programs = doc.getElementsByTagName("program");
		for (int dap = 0; dap < programs.getLength(); dap++) {
			Node c = programs.item(dap);
			Element eElement = (Element) c;
			currentStateIDs.put(eElement.getAttribute(NAME), eElement.getAttribute(ID));
		}

		lookupInitState();
	}

	private void lookupInitState() {

		boolean reboot = getAsBoolean("CCU_im_Reboot");
		if (reboot) {
			ccuInitState = true;
			return;
		}

		String uptime = getAsString("CCU_Uptime");
		long minutesUptime = Duration
				.between(LocalDateTime.parse(uptime, UPTIME_FORMATTER), LocalDateTime.now()).toMinutes();
		ccuInitState = minutesUptime <= INIT_STATE_MINUTES;
	}

	HttpHeaders createHeaders() {

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Accept", "*/*");
		httpHeaders.set("Cache-Control", "no-cache");
		return httpHeaders;
	}

	private Document documentFromUrl(String url) {

		HttpHeaders headers = createHeaders();

		HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
		ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
				String.class);

		String response = responseEntity.getBody();
		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Recieved RC=" + responseEntity.getStatusCode().value() + " from API call:" + url);
		}

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputStream inputStream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
			Document doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			throw new IllegalStateException("Error parsing document", e);
		}
	}

}
