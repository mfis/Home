package de.fimatas.home.adapter.service;

import de.fimatas.home.adapter.accessories.HomekitTemperatureSensor;
import de.fimatas.home.adapter.auth.HomekitAuthentication;
import de.fimatas.home.library.annotation.EnableHomekit;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.AbstractDeviceModel;
import de.fimatas.home.library.domain.model.Climate;
import de.fimatas.home.library.domain.model.HouseModel;
import io.github.hapjava.server.impl.HomekitRoot;
import io.github.hapjava.server.impl.HomekitServer;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Field;

@Component
public class HomekitService {

    private static final Log LOG = LogFactory.getLog(HomekitService.class);

    private static final int BRIDGE_PORT = 9123;

    private HomekitServer homekitServer;

    @Autowired
    private Environment env;

    // @PostConstruct
    public void init() {
        try {
            HomekitAuthentication.getInstance().setPin(env.getProperty("homekit.auth.pin"));

            homekitServer = new HomekitServer(BRIDGE_PORT);
            HomekitRoot homekitBridge = homekitServer.createBridge(HomekitAuthentication.getInstance(),
                    env.getProperty("homekit.bridge.label"),
                    env.getProperty("homekit.bridge.manufacturer"),
                    env.getProperty("homekit.bridge.model"),
                    env.getProperty("homekit.bridge.serialNumber"),
                    env.getProperty("homekit.bridge.firmwareRevision"),
                    env.getProperty("homekit.bridge.hardwareRevision"));

            // HomekitTemperatureSensor livingroom = new HomekitTemperatureSensor();
            // livingroom.updateValue(22.5d);
            // homekitBridge.addAccessory(livingroom);

            homekitBridge.start();

        } catch (Exception e) {
            LOG.error("Unable to start Homekit server.", e);
        }
    }

    @PreDestroy
    public void shutdown() {
       try {
            if(homekitServer!=null){
                homekitServer.stop();
            }
        } catch (Exception e) {
            LOG.error("Unable to shutdown Homekit server.", e);
        }
    }

    @SneakyThrows
    @Async
    public void update()  {

        HouseModel model = ModelObjectDAO.getInstance().readHouseModel();
        if(model == null){
            return;
        }

        LOG.info("MODEL ARRIVED !!!");

        final Field[] fields = HouseModel.class.getDeclaredFields();
        for(Field field : fields){
            if(field.getAnnotation(EnableHomekit.class) != null){
                final AbstractDeviceModel adm = model.lookupField(field.getName(), AbstractDeviceModel.class);
                if(model.getPlaceSubtitles().containsKey(adm.getDevice().getPlace())){
                    LOG.info("TO PROCESS: " + adm.getDevice().getPlace().getPlaceName() + " " + model.getPlaceSubtitles().get(adm.getDevice().getPlace()));
                }else{
                    LOG.info("TO PROCESS: " + adm.getDevice().getPlace().getPlaceName());
                }
            }
        }
    }

}
