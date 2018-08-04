package home.main;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

public class CookieMap {

	private static CookieMap instance;

	private static final Object monitor = new Object();

	private static Map<String, String> map;

	static {
		instance = new CookieMap();
	}

	private Map<String, String> buildNewMap() {
		return Collections.synchronizedMap(new TreeMap<String, String>());
	}

	private CookieMap() {
		map = buildNewMap();
	}

	public static CookieMap getInstance() {
		if (instance == null) {
			synchronized (monitor) {
				if (instance == null) {
					instance = new CookieMap();
				}
			}
		}
		return instance;
	}

	public void loadFrom(Properties properties) {
		for (String key : properties.stringPropertyNames()) {
			if (StringUtils.startsWith(key, "cookie_")) {
				String c = StringUtils.removeStart(key, "cookie_");
				String value = properties.getProperty(key);
				write(c, value);
			}
		}
	}

	public void saveTo(Properties properties) throws Exception {
		for (String key : properties.stringPropertyNames()) {
			if (StringUtils.startsWith(key, "cookie_")) {
				properties.remove(key);
			}
		}
		for (String key : map.keySet()) {
			properties.setProperty("cookie_" + key, map.get(key));
		}
		FileOutputStream fos = new FileOutputStream(new File(properties.getProperty("FILE")));
		properties.store(fos, "");
		fos.flush();
		fos.close();
	}

	public void reset() {
		synchronized (monitor) {
			map = buildNewMap();
		}
	}

	private boolean contains(String key) {
		return key != null && map.containsKey(key);
	}

	public String write(String id, String value) {
		return map.put(id, value);
	}

	public void delete(String id) {
		if (map.containsKey(id)) {
			map.remove(id);
		}
	}

	public String read(String string) {
		if (contains(string)) {
			return StringUtils.trimToNull(map.get(string));
		} else {
			return null;
		}
	}

}
