package home.domain.model;

import java.util.LinkedList;
import java.util.List;

public class ClimateView {

	private String id = "";
	private String name = "";
	private String postfix = "";
	private String stateTemperature = "";
	private String statePostfixIconTemperature = "";
	private String tendencyIconTemperature = "";
	private String stateHumidity = "";
	private String statePostfixIconHumidity = "";
	private String tendencyIconHumidity = "";
	private String colorClass = "secondary";
	private String linkBoost = "";
	private String linkManual = "";
	private String targetTemp = "";
	private String icon = "";
	private String heatericon = "";
	private List<String> hints = new LinkedList<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColorClass() {
		return colorClass;
	}

	public void setColorClass(String colorClass) {
		this.colorClass = colorClass;
	}

	public String getLinkBoost() {
		return linkBoost;
	}

	public void setLinkBoost(String linkBoost) {
		this.linkBoost = linkBoost;
	}

	public String getLinkManual() {
		return linkManual;
	}

	public void setLinkManual(String linkManual) {
		this.linkManual = linkManual;
	}

	public String getTargetTemp() {
		return targetTemp;
	}

	public void setTargetTemp(String targetTemp) {
		this.targetTemp = targetTemp;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getHeatericon() {
		return heatericon;
	}

	public void setHeatericon(String heatericon) {
		this.heatericon = heatericon;
	}

	public String getPostfix() {
		return postfix;
	}

	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}

	public List<String> getHints() {
		return hints;
	}

	public void setHints(List<String> hints) {
		this.hints = hints;
	}

	public String getStateTemperature() {
		return stateTemperature;
	}

	public void setStateTemperature(String stateTemperature) {
		this.stateTemperature = stateTemperature;
	}

	public String getStatePostfixIconTemperature() {
		return statePostfixIconTemperature;
	}

	public void setStatePostfixIconTemperature(String statePostfixIconTemperature) {
		this.statePostfixIconTemperature = statePostfixIconTemperature;
	}

	public String getTendencyIconTemperature() {
		return tendencyIconTemperature;
	}

	public void setTendencyIconTemperature(String tendencyIconTemperature) {
		this.tendencyIconTemperature = tendencyIconTemperature;
	}

	public String getStateHumidity() {
		return stateHumidity;
	}

	public void setStateHumidity(String stateHumidity) {
		this.stateHumidity = stateHumidity;
	}

	public String getStatePostfixIconHumidity() {
		return statePostfixIconHumidity;
	}

	public void setStatePostfixIconHumidity(String statePostfixIconHumidity) {
		this.statePostfixIconHumidity = statePostfixIconHumidity;
	}

	public String getTendencyIconHumidity() {
		return tendencyIconHumidity;
	}

	public void setTendencyIconHumidity(String tendencyIconHumidity) {
		this.tendencyIconHumidity = tendencyIconHumidity;
	}

}