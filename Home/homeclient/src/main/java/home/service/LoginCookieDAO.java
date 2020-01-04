package home.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoginCookieDAO {

	private static final String FILEPATH = System.getProperty("user.home")
			+ "/documents/config/login.properties";

	private static LoginCookieDAO instance;

	Properties properties = null;

	private Log logger = LogFactory.getLog(LoginCookieDAO.class);

	private LoginCookieDAO() {
		super();
		properties = getApplicationProperties();
	}

	public static synchronized LoginCookieDAO getInstance() {
		if (instance == null) {
			instance = new LoginCookieDAO();
		}
		return instance;
	}

	public synchronized void write(String key, String value) {
		String oldValue = read(key);
		if (StringUtils.equals(oldValue, value)) {
			return;
		}
		properties.setProperty(key, value);
		store();
	}

	public void delete(String key) {
		properties.remove(key);
	}

	public void deleteAll() {
		try {
			FileUtils.writeStringToFile(new File(FILEPATH), "", StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.error("error deleting all properties");
		}
		getApplicationProperties();
	}

	public String read(String key) {
		if (key == null) {
			return null;
		}
		return properties.getProperty(key);
	}

	public Properties getApplicationProperties() {
		properties = new Properties();
		try {
			File file = new File(FILEPATH);
			properties.load(new FileInputStream(file));
			return properties;
		} catch (Exception e) {
			throw new IllegalStateException("Properties could not be loaded", e);
		}
	}

	private void store() {
		try {
			FileOutputStream fos = new FileOutputStream(new File(FILEPATH));
			properties.store(fos, "");
			fos.flush();
			fos.close();
		} catch (IOException ioe) {
			throw new IllegalStateException("error writing external properties:", ioe);
		}
	}

}
