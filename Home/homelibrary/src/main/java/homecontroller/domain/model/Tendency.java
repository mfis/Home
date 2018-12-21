package homecontroller.domain.model;

import java.math.BigDecimal;

public enum Tendency {

	RISE(0), //
	RISE_SLIGHT(Constants.ONE_MINUTE * 30), //
	EQUAL(Constants.ONE_MINUTE * 60), //
	FALL_SLIGHT(Constants.ONE_MINUTE * 30), //
	FALL(0), //
	;

	private long timeDiff;

	private Tendency(long timeDiff) {
		this.timeDiff = timeDiff;
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

		public static final long ONE_MINUTE = 1000 * 60;
	}

	public long getTimeDiff() {
		return timeDiff;
	}
}
