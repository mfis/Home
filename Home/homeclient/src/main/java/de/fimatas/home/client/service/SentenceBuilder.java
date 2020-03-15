package de.fimatas.home.client.service;

import org.apache.commons.lang3.StringUtils;

public class SentenceBuilder {

	private StringBuilder sb = new StringBuilder(200);

	private int wordCount = 0;

	private boolean isNewSentence = true;

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
		isNewSentence = false;
		return this;
	}

	public SentenceBuilder newSentence() {
		if (!isNewSentence) {
			sb.append(". ");
			isNewSentence = true;
		}
		return this;
	}

	public String getText() {
		newSentence();
		return sb.toString();
	}

}
