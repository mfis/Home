package de.fimatas.home.client.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class HomeViewModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String timestamp;

    private String defaultAccent;

    private List<HomeViewPlaceModel> places = new LinkedList<>();

    public class HomeViewPlaceModel implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;

        private String name;

        private List<String> placeDirectives = new LinkedList<>();

        private List<HomeViewValueModel> values = new LinkedList<>();

        private List<List<HomeViewActionModel>> actions = new LinkedList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<HomeViewValueModel> getValues() {
            return values;
        }

        public void setValues(List<HomeViewValueModel> values) {
            this.values = values;
        }

        public List<List<HomeViewActionModel>> getActions() {
            return actions;
        }

        public void setActions(List<List<HomeViewActionModel>> actions) {
            this.actions = actions;
        }

        public List<String> getPlaceDirectives() {
            return placeDirectives;
        }

        public void setPlaceDirectives(List<String> placeDirectives) {
            this.placeDirectives = placeDirectives;
        }
    }

    public class HomeViewValueModel implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;

        private String key;

        private String symbol = "";

        private String value;

        private String accent;

        private String tendency = "";

        private List<String> valueDirectives = new LinkedList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getAccent() {
            return accent;
        }

        public void setAccent(String accent) {
            this.accent = accent;
        }

        public String getTendency() {
            return tendency;
        }

        public void setTendency(String tendency) {
            this.tendency = tendency;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public List<String> getValueDirectives() {
            return valueDirectives;
        }

        public void setValueDirectives(List<String> valueDirectives) {
            this.valueDirectives = valueDirectives;
        }
    }

    public class HomeViewActionModel implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;

        private String name;

        private String link;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<HomeViewPlaceModel> getPlaces() {
        return places;
    }

    public void setPlaces(List<HomeViewPlaceModel> places) {
        this.places = places;
    }

    public String getDefaultAccent() {
        return defaultAccent;
    }

    public void setDefaultAccent(String defaultAccent) {
        this.defaultAccent = defaultAccent;
    }

}
