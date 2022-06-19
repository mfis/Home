package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.PresenceModel;
import de.fimatas.home.library.model.PresenceState;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@CommonsLog
public class PresenceService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    private final String STATEHANDLER_GROUPNAME_PERSONS = "presence-persons";

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                refresh();
            } catch (Exception e) {
                log.error("Could not initialize PresenceService completly.", e);
            }
        });
    }

    @Scheduled(fixedDelay = (1000 * HomeAppConstants.MODEL_PRESENCE_INTERVAL_SECONDS) + 200, initialDelay = 20000)
    private void scheduledRefresh() {
        refresh();
    }

    public void refresh() {

        final List<State> states = stateHandlerDAO.readStates(STATEHANDLER_GROUPNAME_PERSONS);

        var newModel = new PresenceModel();
        states.forEach(s-> newModel.getPresenceStates().put(s.getStatename(), PresenceState.valueOf(s.getValue())));

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    public void update(String username, PresenceState state){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_PERSONS, username, state.name());
        refresh();
    }

}
