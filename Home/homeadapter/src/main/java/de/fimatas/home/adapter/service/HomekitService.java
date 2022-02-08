package de.fimatas.home.adapter.service;

import de.fimatas.home.adapter.accessories.HomekitTemperatureSensor;
import de.fimatas.home.adapter.auth.HomekitAuthentication;
import io.github.hapjava.server.impl.HomekitRoot;
import io.github.hapjava.server.impl.HomekitServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class HomekitService {

    private static final Log LOG = LogFactory.getLog(HomekitService.class);

    private static final int BRIDGE_PORT = 9123;

    private HomekitServer homekitServer;

    @Autowired
    private Environment env;

    @PostConstruct
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

}
