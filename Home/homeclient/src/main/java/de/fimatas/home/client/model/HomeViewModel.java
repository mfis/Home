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

        private String valueShort = "";

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
}
