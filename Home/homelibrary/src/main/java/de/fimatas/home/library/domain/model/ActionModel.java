package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class ActionModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private String status;

	public ActionModel() {
		super();
	}

	public ActionModel(String status) {
		super();
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
