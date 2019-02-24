package homecontroller.domain.model;

import java.io.Serializable;

public class OutdoorClimate extends Climate implements Serializable {

	private static final long serialVersionUID = 1L;

	private Intensity sunBeamIntensity;

	private Intensity sunHeatingInContrastToShadeIntensity;

	private OutdoorClimate maxSideSunHeating;

	public Intensity getSunBeamIntensity() {
		return sunBeamIntensity;
	}

	public void setSunBeamIntensity(Intensity sunBeamIntensity) {
		this.sunBeamIntensity = sunBeamIntensity;
	}

	public Intensity getSunHeatingInContrastToShadeIntensity() {
		return sunHeatingInContrastToShadeIntensity;
	}

	public void setSunHeatingInContrastToShadeIntensity(Intensity sunHeatinginContrastToShadeIntensity) {
		this.sunHeatingInContrastToShadeIntensity = sunHeatinginContrastToShadeIntensity;
	}

	public OutdoorClimate getMaxSideSunHeating() {
		return maxSideSunHeating;
	}

	public void setMaxSideSunHeating(OutdoorClimate maxSideSunHeating) {
		this.maxSideSunHeating = maxSideSunHeating;
	}

}
