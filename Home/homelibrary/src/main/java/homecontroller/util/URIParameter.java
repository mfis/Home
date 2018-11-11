package homecontroller.util;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class URIParameter {

	private MultiValueMap<String, String> map;

	public URIParameter() {
		map = new LinkedMultiValueMap<>();
	}

	public URIParameter add(String key, String value) {
		map.add(key, value);
		return this;
	}

	public MultiValueMap<String, String> build() {
		return map;
	}
}
