package homecontroller.domain.model;

import java.math.BigDecimal;

public enum Tendency {

	NONE(0, ""), //

	RISE(0, "far fa-arrow-alt-circle-up"), //
	RISE_SLIGHT(Constants.ONE_MINUTE * 30, "far fa-arrow-alt-circle-up fa-rotate-45"), //
	EQUAL(Constants.ONE_MINUTE * 60, "far fa-arrow-alt-circle-right"), //
	FALL_SLIGHT(Constants.ONE_MINUTE * 30, "far fa-arrow-alt-circle-right fa-rotate-45"), //
	FALL(0, "far fa-arrow-alt-circle-down"), //
	;

	private long timeDiff;

	private String iconCssClass;

	private Tendency(long timeDiff, String iconCssClass) {
		this.timeDiff = timeDiff;
		this.iconCssClass = iconCssClass;
	}

	public static Tendency calculate(ValueWithTendency<BigDecimal> reference, long timeDiff) {

		if (reference.getTendency() == null) {
			return Tendency.EQUAL;
		}

		switch (reference.getTendency()) {
		case RISE:
			if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
				return Tendency.EQUAL;
			} else if (timeDiff >= Tendency.RISE_SLIGHT.getTimeDiff()) {
				return Tendency.RISE_SLIGHT;
			} else {
				return Tendency.RISE;
			}
		case FALL:
			if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
				return Tendency.EQUAL;
			} else if (timeDiff >= Tendency.FALL_SLIGHT.getTimeDiff()) {
				return Tendency.FALL_SLIGHT;
			} else {
				return Tendency.FALL;
			}
		case RISE_SLIGHT:
			if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
				return Tendency.EQUAL;
			} else {
				return Tendency.RISE_SLIGHT;
			}
		case FALL_SLIGHT:
			if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
				return Tendency.EQUAL;
			} else {
				return Tendency.FALL_SLIGHT;
			}
		default:
			return Tendency.EQUAL;
		}
	}

	public static class Constants {

		private Constants() {
			super();
		}

		public static final long ONE_MINUTE = 1000L * 60L;
	}

	public long getTimeDiff() {
		return timeDiff;
	}

	public String getIconCssClass() {
		return iconCssClass;
	}
}
