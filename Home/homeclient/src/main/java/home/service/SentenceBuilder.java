package home.service;

import org.apache.commons.lang3.StringUtils;

public class SentenceBuilder {

	private StringBuilder sb = new StringBuilder(200);

	private int wordCount = 0;

	private SentenceBuilder() {
		super();
	}

	public static SentenceBuilder newInstance() {
		return new SentenceBuilder();
	}

	public SentenceBuilder add(String word) {
		if (wordCount > 0) {
			sb.append(StringUtils.SPACE);
		}
		sb.append(word.trim());
		wordCount++;
		return this;
	}

	public String getSentence() {
		sb.append(". ");
		return sb.toString();
	}

}
