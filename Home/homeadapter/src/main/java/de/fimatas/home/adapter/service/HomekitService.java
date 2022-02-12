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
import io.github.hapjava.server.impl.HomekitRoot;
import io.github.hapjava.server.impl.HomekitServer;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostConstruct
    public void init() {
        try {
            HomekitAuthentication.getInstance().setPin(env.getProperty("homekit.auth.pin"));
            homekitServer = new HomekitServer(Integer.parseInt(Objects.requireNonNull(env.getProperty("homekit.bridge.port"))));
            homekitBridge = homekitServer.createBridge(HomekitAuthentication.getInstance(),
                    env.getProperty("homekit.bridge.label"),
                    env.getProperty("homekit.bridge.manufacturer"),
                    env.getProperty("homekit.bridge.model"),
                    env.getProperty("homekit.bridge.serialNumber"),
                    env.getProperty("homekit.bridge.firmwareRevision"),
                    env.getProperty("homekit.bridge.hardwareRevision"));

        } catch (Exception e) {
            log.error("Unable to start Homekit server.", e);
        }
    }

    @PreDestroy
    public void shutdown() {
       try {
           if(homekitBridge != null){
               homekitBridge.stop();
           }
           if(homekitServer != null){
                homekitServer.stop();
           }
           accessories.clear();
        } catch (Exception e) {
            log.error("Unable to shutdown Homekit server.", e);
        }
    }

    @SneakyThrows
    @Async
    public synchronized void update()  {

        HouseModel model = ModelObjectDAO.getInstance().readHouseModel();
        if(model == null || homekitBridge == null){
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

        if(!bridgeStarted && !accessories.isEmpty()){
            if(Boolean.parseBoolean(env.getProperty("homekit.doLoggingOnly"))){
                logging();
            }else{
                homekitBridge.start();
                bridgeStarted = true;
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
        homekitBridge.addAccessory(accessory);
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
