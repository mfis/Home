package homecontroller.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

	@Autowired
	private Environment env;

	@Autowired
	private RestTemplate restTemplate;

	private String host;

	private Map<String, String> currentValues;
	private Map<String, String> currentStateIDs;

	@PostConstruct
	public void init() {
		host = env.getProperty("homematic.hostName");
	}

	public String getAsString(String key) {
		if (currentValues.containsKey(key)) {
			return currentValues.get(key);
		} else {
			return null;
		}
	}

	public Boolean getAsBoolean(String key) {
		if (currentValues.containsKey(key)) {
			return Boolean.valueOf(currentValues.get(key));
		} else {
			return null;
		}
	}

	public BigDecimal getAsBigDecimal(String key) {
		if (currentValues.containsKey(key)) {
			return new BigDecimal(currentValues.get(key));
		} else {
			return null;
		}
	}

	public void changeValue(String key, String value) {
		String iseID = currentStateIDs.get(key);
		String url = host + "/addons/xmlapi/statechange.cgi?ise_id=" + iseID + "&new_value=" + value;
		documentFromUrl(url);
	}

	public void toggleBooleanState(String key) {

		changeValue("refreshadress", env.getProperty("refresh.adress"));

		String iseID = currentStateIDs.get(key);
		String url = host + "/addons/xmlapi/statechange.cgi?ise_id=" + iseID + "&new_value="
				+ Boolean.toString(!getAsBoolean(key));
		documentFromUrl(url);
	}

	public synchronized void runProgram(String name) {

		changeValue("refreshadress", env.getProperty("refresh.adress"));

		String id = currentStateIDs.get(name);
		String url = host + "/addons/xmlapi/runprogram.cgi?program_id=" + id;
		documentFromUrl(url);
	}

	public void refresh() {

		currentValues = new HashMap<>();
		currentStateIDs = new HashMap<>();

		Document doc = documentFromUrl(host + "/addons/xmlapi/statelist.cgi");
		NodeList datapoints = doc.getElementsByTagName("datapoint");
		for (int dap = 0; dap < datapoints.getLength(); dap++) {
			Node c = datapoints.item(dap);
			Element eElement = (Element) c;
			if (eElement.getAttribute("value") != null && eElement.getAttribute("value").length() > 0) {
				currentValues.put(eElement.getAttribute("name"), eElement.getAttribute("value"));
			}
			if (eElement.getAttribute("type") != null
					&& eElement.getAttribute("type").equalsIgnoreCase("STATE")) {
				currentStateIDs.put(eElement.getAttribute("name"), eElement.getAttribute("ise_id"));
			}
		}

		doc = documentFromUrl(host + "/addons/xmlapi/sysvarlist.cgi");
		NodeList systemVariables = doc.getElementsByTagName("systemVariable");
		for (int dap = 0; dap < systemVariables.getLength(); dap++) {
			Node c = systemVariables.item(dap);
			Element eElement = (Element) c;
			if (eElement.getAttribute("value") != null && eElement.getAttribute("value").length() > 0) {
				currentValues.put(eElement.getAttribute("name"), eElement.getAttribute("value"));
			}
			currentStateIDs.put(eElement.getAttribute("name"), eElement.getAttribute("ise_id"));
		}

		doc = documentFromUrl(host + "/addons/xmlapi/programlist.cgi");
		NodeList programs = doc.getElementsByTagName("program");
		for (int dap = 0; dap < programs.getLength(); dap++) {
			Node c = programs.item(dap);
			Element eElement = (Element) c;
			currentStateIDs.put(eElement.getAttribute("name"), eElement.getAttribute("id"));
		}

	}

	HttpHeaders createHeaders() {

		return new HttpHeaders() {
			private static final long serialVersionUID = 1L;
			{
				set("Accept", "*/*");
				set("Cache-Control", "no-cache");
			}
		};
	}

	private Document documentFromUrl(String url) {

		HttpHeaders headers = createHeaders();

		HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
		ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
				String.class);

		String response = responseEntity.getBody();

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputStream inputStream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
			Document doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			throw new RuntimeException("Error parsing document", e);
		}
	}

}
