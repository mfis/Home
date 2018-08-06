package domain;

import java.math.BigDecimal;

public class HouseModel {

	private BigDecimal kidsRoomTemperature;

	private BigDecimal kidsRoomHumidity;

	private BigDecimal bathRoomTemperature;

	private boolean bathRoomBoost;

	private BigDecimal bedRoomTemperature;

	private BigDecimal bedRoomHumidity;

	private BigDecimal livingRoomTemperature;

	private BigDecimal livingRoomHumidity;

	private BigDecimal terraceTemperature;

	private BigDecimal terraceSunHeatingDiff;

	private BigDecimal entranceTemperature;

	private BigDecimal entranceSunHeatingDiff;

	private boolean kitchenLightSwitchState;

	private int houseElectricalPowerConsumption;

	// ----------

	private BigDecimal conclusionFacadeMinTemp;

	private String conclusionFacadeMinTempName;

	private BigDecimal conclusionFacadeSidesDifference;

	private BigDecimal conclusionFacadeMaxTemp;

	private BigDecimal conclusionFacadeMaxTempSunHeating;

	private String conclusionFacadeMaxTempName;

	private Intensity conclusionFacadeMaxTempSunIntensity;

	private Intensity conclusionFacadeMaxTempHeatingIntensity;

	private String conclusionHintKidsRoom;

	private String conclusionHintBathRoom;

	private String conclusionHintBedRoom;

	private String conclusionHintLivingRoom;

	// ----------

	public BigDecimal getKidsRoomTemperature() {
		return kidsRoomTemperature;
	}

	public void setKidsRoomTemperature(BigDecimal kidsRoomTemperature) {
		this.kidsRoomTemperature = kidsRoomTemperature;
	}

	public BigDecimal getKidsRoomHumidity() {
		return kidsRoomHumidity;
	}

	public void setKidsRoomHumidity(BigDecimal kidsRoomHumidity) {
		this.kidsRoomHumidity = kidsRoomHumidity;
	}

	public BigDecimal getBathRoomTemperature() {
		return bathRoomTemperature;
	}

	public void setBathRoomTemperature(BigDecimal bathRoomTemperature) {
		this.bathRoomTemperature = bathRoomTemperature;
	}

	public BigDecimal getBedRoomTemperature() {
		return bedRoomTemperature;
	}

	public void setBedRoomTemperature(BigDecimal bedRoomTemperature) {
		this.bedRoomTemperature = bedRoomTemperature;
	}

	public BigDecimal getBedRoomHumidity() {
		return bedRoomHumidity;
	}

	public void setBedRoomHumidity(BigDecimal bedRoomHumidity) {
		this.bedRoomHumidity = bedRoomHumidity;
	}

	public BigDecimal getLivingRoomTemperature() {
		return livingRoomTemperature;
	}

	public void setLivingRoomTemperature(BigDecimal livingRoomTemperature) {
		this.livingRoomTemperature = livingRoomTemperature;
	}

	public BigDecimal getLivingRoomHumidity() {
		return livingRoomHumidity;
	}

	public void setLivingRoomHumidity(BigDecimal livingRoomHumidity) {
		this.livingRoomHumidity = livingRoomHumidity;
	}

	public BigDecimal getTerraceTemperature() {
		return terraceTemperature;
	}

	public void setTerraceTemperature(BigDecimal terraceTemperature) {
		this.terraceTemperature = terraceTemperature;
	}

	public BigDecimal getTerraceSunHeatingDiff() {
		return terraceSunHeatingDiff;
	}

	public void setTerraceSunHeatingDiff(BigDecimal terraceSunHeatingDiff) {
		this.terraceSunHeatingDiff = terraceSunHeatingDiff;
	}

	public BigDecimal getEntranceTemperature() {
		return entranceTemperature;
	}

	public void setEntranceTemperature(BigDecimal entranceTemperature) {
		this.entranceTemperature = entranceTemperature;
	}

	public BigDecimal getEntranceSunHeatingDiff() {
		return entranceSunHeatingDiff;
	}

	public void setEntranceSunHeatingDiff(BigDecimal entranceSunHeatingDiff) {
		this.entranceSunHeatingDiff = entranceSunHeatingDiff;
	}

	public int getHouseElectricalPowerConsumption() {
		return houseElectricalPowerConsumption;
	}

	public void setHouseElectricalPowerConsumption(int houseElectricalPowerConsumption) {
		this.houseElectricalPowerConsumption = houseElectricalPowerConsumption;
	}

	public boolean isBathRoomBoost() {
		return bathRoomBoost;
	}

	public void setBathRoomBoost(boolean bathRoomBoost) {
		this.bathRoomBoost = bathRoomBoost;
	}

	public boolean isKitchenLightSwitchState() {
		return kitchenLightSwitchState;
	}

	public void setKitchenLightSwitchState(boolean kitchenLightSwitchState) {
		this.kitchenLightSwitchState = kitchenLightSwitchState;
	}

	public BigDecimal getConclusionFacadeMinTemp() {
		return conclusionFacadeMinTemp;
	}

	public void setConclusionFacadeMinTemp(BigDecimal conclusionFacadeMinTemp) {
		this.conclusionFacadeMinTemp = conclusionFacadeMinTemp;
	}

	public String getConclusionFacadeMinTempName() {
		return conclusionFacadeMinTempName;
	}

	public void setConclusionFacadeMinTempName(String conclusionFacadeMinTempName) {
		this.conclusionFacadeMinTempName = conclusionFacadeMinTempName;
	}

	public BigDecimal getConclusionFacadeSidesDifference() {
		return conclusionFacadeSidesDifference;
	}

	public void setConclusionFacadeSidesDifference(BigDecimal conclusionFacadeSidesDifference) {
		this.conclusionFacadeSidesDifference = conclusionFacadeSidesDifference;
	}

	public BigDecimal getConclusionFacadeMaxTemp() {
		return conclusionFacadeMaxTemp;
	}

	public void setConclusionFacadeMaxTemp(BigDecimal conclusionFacadeMaxTemp) {
		this.conclusionFacadeMaxTemp = conclusionFacadeMaxTemp;
	}

	public BigDecimal getConclusionFacadeMaxTempSunHeating() {
		return conclusionFacadeMaxTempSunHeating;
	}

	public void setConclusionFacadeMaxTempSunHeating(BigDecimal conclusionFacadeMaxTempSunHeating) {
		this.conclusionFacadeMaxTempSunHeating = conclusionFacadeMaxTempSunHeating;
	}

	public String getConclusionFacadeMaxTempName() {
		return conclusionFacadeMaxTempName;
	}

	public void setConclusionFacadeMaxTempName(String conclusionFacadeMaxTempName) {
		this.conclusionFacadeMaxTempName = conclusionFacadeMaxTempName;
	}

	public Intensity getConclusionFacadeMaxTempSunIntensity() {
		return conclusionFacadeMaxTempSunIntensity;
	}

	public void setConclusionFacadeMaxTempSunIntensity(Intensity conclusionFacadeMaxTempSunIntensity) {
		this.conclusionFacadeMaxTempSunIntensity = conclusionFacadeMaxTempSunIntensity;
	}

	public Intensity getConclusionFacadeMaxTempHeatingIntensity() {
		return conclusionFacadeMaxTempHeatingIntensity;
	}

	public void setConclusionFacadeMaxTempHeatingIntensity(Intensity conclusionFacadeMaxTempHeatingIntensity) {
		this.conclusionFacadeMaxTempHeatingIntensity = conclusionFacadeMaxTempHeatingIntensity;
	}

	public String getConclusionHintKidsRoom() {
		return conclusionHintKidsRoom;
	}

	public void setConclusionHintKidsRoom(String conclusionHintKidsRoom) {
		this.conclusionHintKidsRoom = conclusionHintKidsRoom;
	}

	public String getConclusionHintBathRoom() {
		return conclusionHintBathRoom;
	}

	public void setConclusionHintBathRoom(String conclusionHintBathRoom) {
		this.conclusionHintBathRoom = conclusionHintBathRoom;
	}

	public String getConclusionHintBedRoom() {
		return conclusionHintBedRoom;
	}

	public void setConclusionHintBedRoom(String conclusionHintBedRoom) {
		this.conclusionHintBedRoom = conclusionHintBedRoom;
	}

	public String getConclusionHintLivingRoom() {
		return conclusionHintLivingRoom;
	}

	public void setConclusionHintLivingRoom(String conclusionHintLivingRoom) {
		this.conclusionHintLivingRoom = conclusionHintLivingRoom;
	}
}
