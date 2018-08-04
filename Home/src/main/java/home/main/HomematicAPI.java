package home.main;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HomematicAPI {

	private String host;
	private List<String> hmDevicePrefixes;

	private Map<String, String> currentValues;
	private Map<String, String> currentStateIDs;

	public HomematicAPI(String host, List<String> hmDevicePrefixes) {
		this.host = host;
		this.hmDevicePrefixes = hmDevicePrefixes;
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

	public String getAsFormattedBigDecimal(String key) {
		if (currentValues.containsKey(key)) {
			return new DecimalFormat("0.#").format(new BigDecimal(currentValues.get(key)));
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

	public void toggleBooleanState(String key) {

		if (hmDevicePrefixes.contains(StringUtils.substringBefore(key, ".")) && !key.endsWith(".STATE")) {
			key += ".STATE";
		}
		String iseID = currentStateIDs.get(key);
		String url = "https://" + host + "/addons/xmlapi/statechange.cgi?ise_id=" + iseID + "&new_value=" + Boolean.toString(!getAsBoolean(key));
		documentFromUrl(url);
	}

	public void refresh() {

		currentValues = new HashMap<>();
		currentStateIDs = new HashMap<>();

		Document doc = documentFromUrl("https://" + host + "/addons/xmlapi/statelist.cgi");
		NodeList datapoints = doc.getElementsByTagName("datapoint");
		for (int dap = 0; dap < datapoints.getLength(); dap++) {
			Node c = datapoints.item(dap);
			Element eElement = (Element) c;
			if (eElement.getAttribute("value") != null && eElement.getAttribute("value").length() > 0) {
				currentValues.put(eElement.getAttribute("name"), eElement.getAttribute("value"));
			}
			if (eElement.getAttribute("type") != null && eElement.getAttribute("type").equalsIgnoreCase("STATE")) {
				currentStateIDs.put(eElement.getAttribute("name"), eElement.getAttribute("ise_id"));
			}
		}

		doc = documentFromUrl("https://" + host + "/addons/xmlapi/sysvarlist.cgi");
		NodeList systemVariables = doc.getElementsByTagName("systemVariable");
		for (int dap = 0; dap < systemVariables.getLength(); dap++) {
			Node c = systemVariables.item(dap);
			Element eElement = (Element) c;
			if (eElement.getAttribute("value") != null && eElement.getAttribute("value").length() > 0) {
				currentValues.put(eElement.getAttribute("name"), eElement.getAttribute("value"));
			}
			currentStateIDs.put(eElement.getAttribute("name"), eElement.getAttribute("ise_id"));
		}

	}

	HttpHeaders createHeaders() {

		return new HttpHeaders() {
			private static final long serialVersionUID = 1L;
			{
				String plainClientCredentials = ExternalPropertiesDAO.getInstance().read("xmlapi.auth.user") + ":" + ExternalPropertiesDAO.getInstance().read("xmlapi.auth.pass");
				String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));
				set("Authorization", "Basic " + base64ClientCredentials);
				set("Accept", "*/*");
				set("Cache-Control", "no-cache");
			}
		};
	}

	private Document documentFromUrl(String url) {

		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = createHeaders();

		HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
		ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.GET, requestEntity, String.class);

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
