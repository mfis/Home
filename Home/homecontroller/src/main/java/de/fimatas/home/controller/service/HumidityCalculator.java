package de.fimatas.home.controller.service;

import org.springframework.stereotype.Component;

@Component
public class HumidityCalculator {

	private static final double PERCENT_BASE = 100.0;
	private static final double AZ_CELSIUS = 273.15;
	private static final double MOLAR_MASS_STEAM = 18.0153;
	private static final double GAS_CONSTANT_R = 8.3145;
	private static final double MAGNUS_FORMULA_E0 = 6.1078;
	private static final double MAGNUS_FORMULA_A_PLUS = 7.5;
	private static final double MAGNUS_FORMULA_A_MINUS = 7.6;
	private static final double MAGNUS_FORMULA_B_PLUS = 237.3;
	private static final double MAGNUS_FORMULA_B_MINUS = 240.7;

	public double relToAbs(double temp, double relH) {
		double kelvin = toKelvin(temp);
		double svp = saturatedVaporPressure(temp);
		return (MOLAR_MASS_STEAM / (GAS_CONSTANT_R / PERCENT_BASE)) * svp * (relH / PERCENT_BASE / kelvin);
	}

	public double absToRel(double temp, double absH) {
		double kelvin = toKelvin(temp);
		double svp = saturatedVaporPressure(temp);
		return (PERCENT_BASE * absH * (GAS_CONSTANT_R / PERCENT_BASE) * kelvin) / (MOLAR_MASS_STEAM * svp);
	}

	private double toKelvin(double temp) {
		return temp + AZ_CELSIUS;
	}

	private double saturatedVaporPressure(double temp) {
		return MAGNUS_FORMULA_E0 * Math.pow(10, ((temp < 0 ? MAGNUS_FORMULA_A_MINUS : MAGNUS_FORMULA_A_PLUS) * temp)
				/ ((temp < 0 ? MAGNUS_FORMULA_B_MINUS : MAGNUS_FORMULA_B_PLUS) + temp));
	}

}
