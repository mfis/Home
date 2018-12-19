package homecontroller.domain.model;

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
