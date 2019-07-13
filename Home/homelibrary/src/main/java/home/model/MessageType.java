package home.model;

public enum MessageType {

	REFRESH_ALL_MODELS(null), //
	TOGGLESTATE(Pages.PATH_HOME), //
	TOGGLEAUTOMATION(Pages.PATH_HOME), //
	;//

	private final String targetSite;

	private MessageType(String targetSite) {
		this.targetSite = targetSite;
	}

	public String getTargetSite() {
		return targetSite;
	}
}
