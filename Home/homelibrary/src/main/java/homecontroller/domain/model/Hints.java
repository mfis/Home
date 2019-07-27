package homecontroller.domain.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Hints implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<Hint, HintState> hintMap = new LinkedHashMap<>();
	private Map<Hint, Long> timeMap = new LinkedHashMap<>();

	public void overtakeOldHints(Hints old, long actualTimeMillies) {
		for (Hint hint : old.timeMap.keySet()) {
			timeMap.put(hint, old.timeMap.get(hint));
		}
		for (Hint hint : old.hintMap.keySet()) {
			HintState oldState = old.hintMap.get(hint);
			HintState newState = oldState.down(actualTimeMillies - old.timeMap.get(hint));
			hintMap.put(hint, newState);
			if (newState != oldState) {
				timeMap.put(hint, actualTimeMillies);
			}
		}
	}

	public void giveHint(Hint hint, long actualTimeMillies) {
		if (hintMap.containsKey(hint)) {
			HintState oldState = hintMap.get(hint);
			HintState newState = oldState.up(actualTimeMillies - timeMap.get(hint));
			hintMap.put(hint, newState);
			if (newState != oldState) {
				timeMap.put(hint, actualTimeMillies);
			}
		} else {
			hintMap.put(hint, HintState.newState());
			timeMap.put(hint, actualTimeMillies);
		}
	}

	public List<String> formatAsText(boolean onlySolid, boolean withRoomName, RoomClimate roomClimate) {
		List<String> list = new LinkedList<>();
		for (Hint hint : getHintMap().keySet()) {
			if (getHintMap().get(hint) == HintState.OFF) {
				// skip always OFF state
			} else if (onlySolid && (getHintMap().get(hint) == HintState.OFF_HYSTERESIS)) {
				// skip
			} else {
				if (withRoomName) {
					list.add(hint.formatWithRoomName(roomClimate));
				} else {
					list.add(getHintMap().get(hint).getTextualPrefix() + " " + hint.getText());
				}
			}
		}
		return list;
	}

	public Map<Hint, HintState> getHintMap() {
		return hintMap;
	}

	public void setHintMap(Map<Hint, HintState> hintMap) {
		this.hintMap = hintMap;
	}

	public Map<Hint, Long> getTimeMap() {
		return timeMap;
	}

	public void setTimeMap(Map<Hint, Long> timeMap) {
		this.timeMap = timeMap;
	}
}
