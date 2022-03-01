package de.fimatas.home.adapter.service;

import de.fimatas.home.adapter.accessories.HomekitTemperatureSensor;
import de.fimatas.home.adapter.auth.HomekitAuthentication;
import de.fimatas.home.adapter.model.HomekitAccessoryWithModelField;
import de.fimatas.home.library.annotation.EnableHomekit;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.AbstractDeviceModel;
import de.fimatas.home.library.domain.model.Climate;
import de.fimatas.home.library.domain.model.HouseModel;
import io.github.hapjava.accessories.TemperatureSensorAccessory;
import io.github.hapjava.server.HomekitAccessoryCategories;
import io.github.hapjava.server.impl.HomekitRoot;
import io.github.hapjava.server.impl.HomekitServer;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@CommonsLog
public class HomekitService {

    @Autowired
    private Environment env;

    private HomekitServer homekitServer;
    private HomekitRoot homekitBridge;
    private boolean bridgeStarted;

    private final Map<Integer, HomekitAccessoryWithModelField> accessories = new HashMap<>();

    @Value("${homekit.doLoggingOnly}")
    private boolean doLoggingOnly;

    @PostConstruct
    public void init() {
        HomekitAuthentication.getInstance().setPin(env.getProperty("homekit.auth.pin"));
    }

    @PreDestroy
    public void shutdown() {
        stopServer();
    }

    @SneakyThrows
    @Async
    public synchronized void update()  {

        HouseModel model = ModelObjectDAO.getInstance().readHouseModel();
        if(model == null){
            log.warn("Update interrupted.");
            return;
        }

        for(Field field : HouseModel.class.getDeclaredFields()){
            final EnableHomekit enableHomekit = field.getAnnotation(EnableHomekit.class);
            if(enableHomekit != null){
                if(!accessories.containsKey(enableHomekit.accessoryId())){
                    registerNewAccessory(field, enableHomekit);
                }
            }
        }

        if(accessories.values().stream().anyMatch(a -> !a.isAddedToBridge())){
            if(doLoggingOnly){
                logging();
            }else{
                if(bridgeStarted){
                    stopServer();
                }
                startServer();
            }
        }

        accessories.values().forEach(HomekitAccessoryWithModelField::checkValueUpdate);
    }

    private void registerNewAccessory(final Field field, final EnableHomekit enableHomekit) {

        HouseModel model = ModelObjectDAO.getInstance().readHouseModel();
        final AbstractDeviceModel adm = model.lookupField(field.getName(), AbstractDeviceModel.class);
        HomekitAccessoryWithModelField accessory;

        if(adm instanceof Climate){
            accessory = new HomekitTemperatureSensor();
        }else{
            return;
        }

        accessory.setModelFieldName(field.getName());
        accessory.setAccessoryId(enableHomekit.accessoryId());
        accessories.put(enableHomekit.accessoryId(), accessory);
    }

    private void startServer() {
        log.info("Starting server...");
        try {
            homekitServer = new HomekitServer(Integer.parseInt(Objects.requireNonNull(env.getProperty("homekit.bridge.port"))));
            homekitBridge = homekitServer.createBridge(HomekitAuthentication.getInstance(),
                    env.getProperty("homekit.bridge.label"),
                    HomekitAccessoryCategories.BRIDGES,
                    env.getProperty("homekit.bridge.manufacturer"),
                    env.getProperty("homekit.bridge.model"),
                    env.getProperty("homekit.bridge.serialNumber"),
                    env.getProperty("homekit.bridge.firmwareRevision"),
                    env.getProperty("homekit.bridge.hardwareRevision"));

        } catch (Exception e) {
            log.error("Unable to start Homekit server.", e);
        }
        accessories.values().forEach(a -> {
            homekitBridge.addAccessory(a);
            a.setAddedToBridge(true);
        });
        log.info("Starting bridge...");
        homekitBridge.start();
        bridgeStarted = true;
    }

    private void stopServer() {
        try {
            if(homekitBridge != null){
                homekitBridge.stop();
            }
            if(homekitServer != null){
                homekitServer.stop();
            }
            accessories.values().forEach(a -> a.setAddedToBridge(false));
            bridgeStarted = false;
        } catch (Exception e) {
            log.error("Unable to shutdown Homekit server.", e);
        }
    }

    @SneakyThrows
    private void logging() {
        for (HomekitAccessoryWithModelField accessory: accessories.values()) {
            if(accessory instanceof TemperatureSensorAccessory){
                log.info(accessory.getName().get() + ": "
                        + ((TemperatureSensorAccessory) accessory).getCurrentTemperature().get());
            }
        }
    }

}
