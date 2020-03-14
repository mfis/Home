package homecontroller.service;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import homelibrary.homematic.model.Device;

@Component
public class DeviceQualifier {

	@Autowired
	private Environment env;
	
	private static final String PROPERTY_PREFIX = "device.qualifier.";
	
	private static final String PROPERTY_SEPARATOR = ":";

	private HashMap<String, HomematicQualifiers> map = new HashMap<>();

	public String idFrom(Device device) {
		return get(device).id;
	}

	public Integer channelFrom(Device device) {
		return get(device).channel;
	}
	
	private HomematicQualifiers get(Device device) {
		
		if(map.containsKey(device.name())) {
			return map.get(device.name());
		}else {
			read(device);
			return map.get(device.name());
		}
	}

	private void read(Device device) {
		
		var key = PROPERTY_PREFIX + device.name();
		var property = env.getProperty(key);
		assertNotNull("Property '" + key + "' not set!", property);
		HomematicQualifiers hq = new HomematicQualifiers();
		hq.id = StringUtils.substringBefore(property, PROPERTY_SEPARATOR).trim();
		hq.channel = Integer.parseInt(StringUtils.substringAfter(property, PROPERTY_SEPARATOR).trim());
		map.put(device.name(), hq);
	}

	private class HomematicQualifiers {
		private String id;
		private Integer channel;
	}

}
