package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class TemperatureHistory implements Serializable {

	private static final long serialVersionUID = 1L;

	private long date;

	private boolean singleDay;

	private BigDecimal nightMin;

	private BigDecimal nightMax;

	private BigDecimal dayMin;

	private BigDecimal dayMax;

	public boolean empty() {
		return nightMin == null && nightMax == null && dayMin == null && dayMax == null;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public boolean isSingleDay() {
		return singleDay;
	}

	public void setSingleDay(boolean singleDay) {
		this.singleDay = singleDay;
	}

	public BigDecimal getNightMin() {
		return nightMin;
	}

	public void setNightMin(BigDecimal nightMin) {
		this.nightMin = nightMin;
	}

	public BigDecimal getNightMax() {
		return nightMax;
	}

	public void setNightMax(BigDecimal nightMax) {
		this.nightMax = nightMax;
	}

	public BigDecimal getDayMin() {
		return dayMin;
	}

	public void setDayMin(BigDecimal dayMin) {
		this.dayMin = dayMin;
	}

	public BigDecimal getDayMax() {
		return dayMax;
	}

	public void setDayMax(BigDecimal dayMax) {
		this.dayMax = dayMax;
	}
}
