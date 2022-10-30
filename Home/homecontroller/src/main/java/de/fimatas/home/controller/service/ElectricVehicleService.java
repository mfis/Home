package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@CommonsLog
public class ElectricVehicleService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    private final String STATEHANDLER_GROUPNAME_EV = "ev-battery";

    private final int CHARGEPOINT_FIX = 1;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                refresh();
            } catch (Exception e) {
                log.error("Could not initialize ElectricVehicleService completly.", e);
            }
        });
    }

    public void refresh() {

        final List<State> states = stateHandlerDAO.readStates(STATEHANDLER_GROUPNAME_EV);

        var newModel = new ElectricVehicleModel();
        states.forEach(s-> newModel.getEvMap().put(ElectricVehicle.valueOf(s.getStatename()),
                new ElectricVehicleState(ElectricVehicle.valueOf(s.getStatename()), Short.parseShort(s.getValue()), s.getTimestamp())));

        Arrays.stream(ElectricVehicle.values()).filter(ev -> !newModel.getEvMap().containsKey(ev)).forEach(ev ->
                newModel.getEvMap().put(ev, new ElectricVehicleState(ev, (short) 0, LocalDateTime.now())));

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    public void updateBatteryPercentage(ElectricVehicle electricVehicle, String percentageString){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_EV, electricVehicle.name(), Short.toString(Short.parseShort(percentageString)));
        refresh();
    }
}
