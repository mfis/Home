package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.PresenceModel;
import de.fimatas.home.library.model.PresenceState;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@CommonsLog
public class PresenceService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    private final String STATEHANDLER_GROUPNAME_PERSONS = "presence-persons";

    @Scheduled(cron = "20 4/16 * * * *")
    public void scheduledRefresh() {
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
