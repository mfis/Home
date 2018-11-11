package home.service;

import java.util.HashMap;
import java.util.Map;

public class ViewAttributesDAO {

	private static ViewAttributesDAO instance;

	private Map<String, Map<String, String>> attributes = new HashMap<>();

	private static final Object monitor = new Object();

	public final static String Y_POS_HOME = "Y_POS_HOME";

	public final static String USER_NAME = "USER_NAME";

	private ViewAttributesDAO() {
		super();
	}

	public static ViewAttributesDAO getInstance() {
		if (instance == null) {
			synchronized (monitor) {
				if (instance == null) {
					instance = new ViewAttributesDAO();
				}
			}
		}
		return instance;
	}

	public void push(String user, String key, String value) {

		if (!attributes.containsKey(user)) {
			attributes.put(user, new HashMap<>());
		}

		attributes.get(user).put(key, value);
	}

	public String pull(String user, String key) {

		if (!attributes.containsKey(user)) {
			return null;
		}

		if (!attributes.get(user).containsKey(key)) {
			return null;
		}

		String value = attributes.get(user).get(key);
		attributes.get(user).remove(key);
		return value;
	}

}
