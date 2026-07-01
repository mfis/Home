package de.fimatas.home.client.model;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Data
public class HomeViewModel implements Serializable {

    private String timestamp;

    private String defaultAccent;

    private final List<HomeViewPlaceModel> places = new LinkedList<>();

    @Data
    public static class HomeViewPlaceModel implements Serializable {

        private String id;

        private String name;

        private final List<String> placeDirectives = new LinkedList<>();

        private final List<HomeViewValueModel> values = new LinkedList<>();

        private final List<List<HomeViewActionModel>> actions = new LinkedList<>();
    }

    @Data
    public static class HomeViewValueModel implements Serializable {

        private String id;

        private String key;

        private String symbol = EMPTY;

        private String value;

        private String valueShort = EMPTY;

        private String accent;

        private String tendency = EMPTY;

        private final List<String> valueDirectives = new LinkedList<>();
    }

    @Data
    public static class HomeViewActionModel implements Serializable {

        private String id;

        private String name;

        private String link;
    }

    public void sanitizeData() {

        for (HomeViewPlaceModel place : places) {

            if (place.getValues() != null) {
                for (HomeViewValueModel valueModel : place.getValues()) {
                    if (valueModel.getKey() == null || valueModel.getKey().isEmpty()) {
                        valueModel.setKey("???");
                    }
                    if (valueModel.getValue() == null || valueModel.getValue().isEmpty()) {
                        valueModel.setValue("???");
                    }
                }
            }

            if (place.getActions() != null) {
                for (List<HomeViewActionModel> actionList : place.getActions()) {
                    if (actionList != null) {
                        actionList.removeIf(action ->
                                action.getName() == null || action.getLink() == null
                        );
                    }
                }
            }
        }
    }
}
