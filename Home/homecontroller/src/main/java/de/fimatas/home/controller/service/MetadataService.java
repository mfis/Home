package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.library.domain.model.MetadataModel;
import de.fimatas.home.library.domain.model.Place;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@CommonsLog
public class MetadataService {

    @Autowired
    private Environment env;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private HomematicAPI hmApi;

    @Scheduled(initialDelay = 1000,  fixedDelay = 1000 * 60 * 60)
    public void refresh() {
        var metadataModel = new MetadataModel();
        readSubtitles(metadataModel);
        uploadService.uploadToClient(metadataModel);
    }

    public Optional<String> readSubtitleFor(Place place){
        var key = "place." + place.name() + ".subtitle";
        if(env.containsProperty(key)){
            return Optional.ofNullable(env.getProperty(key));
        }
        return Optional.empty();
    }

    private void readSubtitles(MetadataModel metadataModel) {
        for (Place place : Place.values()) {
            Optional<String> subtitle = readSubtitleFor(place);
            subtitle.ifPresent(s -> metadataModel.getPlaceSubtitles().put(place, s));
        }
    }
}
