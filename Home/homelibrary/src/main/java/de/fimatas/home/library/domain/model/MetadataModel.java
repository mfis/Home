package de.fimatas.home.library.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataModel extends AbstractSystemModel {

    private Map<Place, String> placeSubtitles = new LinkedHashMap<>();

    public MetadataModel() {
        super();
        timestamp = new Date().getTime();
    }
}
