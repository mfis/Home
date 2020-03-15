package de.fimatas.home.client.domain.model;

import java.io.Serializable;

public class ValueWithCaption implements Serializable {

	private static final long serialVersionUID = 1L;

	private String value = "";

	private String caption = "";

	private String cssClass = "";

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public String getCssClass() {
		return cssClass;
	}

	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

}
