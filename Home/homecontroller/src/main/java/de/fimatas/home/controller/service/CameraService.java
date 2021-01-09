package de.fimatas.home.controller.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.domain.service.UploadService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.Camera;
import de.fimatas.home.library.domain.model.CameraMode;
import de.fimatas.home.library.domain.model.CameraModel;
import de.fimatas.home.library.domain.model.CameraPicture;
import de.fimatas.home.library.domain.model.Doorbell;
import de.fimatas.home.library.homematic.model.Device;

@Component
public class CameraService {

    @Autowired
    @Qualifier("restTemplateLowTimeout")
    private RestTemplate restTemplateLowTimeout;

    @Autowired
    @Qualifier("restTemplateBinaryResponse")
    private RestTemplate restTemplateBinaryResponse;

    @Autowired
    private HomematicAPI homematicAPI;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private DeviceQualifier deviceQualifier;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private Environment env;

    private static final Log LOG = LogFactory.getLog(CameraService.class);

    private static final Object MONITOR = new Object();

    public void cleanUp() {
        CameraModel cameraModel = ModelObjectDAO.getInstance().readCameraModel();
        if (cameraModel.cleanUp()) {
            LOG.info("Cleaned up camera pictures");
            uploadService.upload(cameraModel);
        }
    }

    public void takeEventPicture(Doorbell frontdoorbell, Camera frontdoorcamera) {
        CameraPicture cameraPicture = new CameraPicture();
        cameraPicture.setTimestamp(frontdoorbell.getTimestampLastDoorbell());
        cameraPicture.setDevice(frontdoorcamera.getDevice());
        cameraPicture.setCameraMode(CameraMode.EVENT);
        takePicture(cameraPicture);
    }

    public String takeLivePicture(Device device) {
        CameraPicture cameraPicture = new CameraPicture();
        cameraPicture.setTimestamp(new Date().getTime());
        cameraPicture.setDevice(device);
        cameraPicture.setCameraMode(CameraMode.LIVE);
        takePicture(cameraPicture);
        return String.valueOf(cameraPicture.getTimestamp());
    }

    private void takePicture(CameraPicture cameraPicture) {

        CompletableFuture.runAsync(() -> {
            synchronized (MONITOR) {
                long l1 = System.currentTimeMillis();
                try {
                    turnOnCamera(cameraPicture.getDevice());
                    byte[] picture = cameraReadPicture(cameraPicture.getDevice());
                    writePicture(cameraPicture, picture);
                } catch (Exception e) {
                    LOG.error("Exception taking picture:", e);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("TIME = " + (System.currentTimeMillis() - l1) + " ms");
                }
            }
        });
    }

    private void writePicture(CameraPicture cameraPicture, byte[] picture) {

        if (picture.length == 0) {
            LOG.error("empty camera image!");
            return;
        }

        cameraPicture.setBytes(picture);
        CameraModel cameraModel = ModelObjectDAO.getInstance().readCameraModel();
        if (cameraPicture.getCameraMode() == CameraMode.LIVE) {
            cameraModel.setLivePicture(cameraPicture);
        } else if (cameraPicture.getCameraMode() == CameraMode.EVENT) {
            LOG.info("writing event picture: " + cameraPicture.getTimestamp());
            cameraModel.getEventPictures().add(cameraPicture); // TODO:
                                                               // DELETE
                                                               // OLDEST
        }
        uploadService.upload(cameraModel);
    }

    private void turnOnCamera(Device deviceSwitch) {

        if (cameraUseMock()) {
            return;
        }

        if (pingCamera(deviceSwitch, false)) {
            LOG.info("Camera already on");
            return;
        }

        LOG.info("Kamera einschalten...");
        homematicAPI.executeCommand(homematicCommandBuilder.exec(deviceSwitch, "DirektEinschalten"));
        boolean pingCameraOk = false;
        long startPolling = System.currentTimeMillis();
        do {
            if (System.currentTimeMillis() - startPolling > (1000L * 20L)) {
                throw new IllegalStateException("camera not started");
            }
            pingCameraOk = pingCamera(deviceSwitch, true);
        } while (!pingCameraOk);
    }

    private boolean pingCamera(Device device, boolean sleepIfNotReachable) {

        boolean pingCameraOk = false;

        try {
            LOG.info("PING");
            String url = env.getProperty("camera." + deviceQualifier.idFrom(device) + ".url");
            ResponseEntity<String> response = restTemplateLowTimeout.getForEntity(url + "/ping", String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                pingCameraOk = true;
            } else {
                sleep(1000, sleepIfNotReachable); // RC=404 etc
            }
        } catch (Exception e) {
            if (isExceptionExpectedTimeout(e)) {
                LOG.info("PING - Timeout");
                sleep(1000, sleepIfNotReachable);
            } else if (isExceptionExpectedHostDown(e)) {
                LOG.info("PING - Host is down");
                sleep(1000, sleepIfNotReachable);
            } else {
                LOG.error("Error ping camera: ", e);
                sleep(1000, sleepIfNotReachable);
            }
        }
        return pingCameraOk;
    }

    private void sleep(long ms, boolean doSleep) {
        try {
            if (doSleep) {
                Thread.sleep(ms);
            }
        } catch (InterruptedException e) { // NOSONAR
            // noop
        }
    }

    private boolean isExceptionExpectedTimeout(Exception e) {
        return e.getClass().isAssignableFrom(ResourceAccessException.class) && e.getCause() != null
            && e.getCause().getClass().isAssignableFrom(ConnectTimeoutException.class) && e.getCause().getCause() != null
            && e.getCause().getCause().getClass().isAssignableFrom(SocketTimeoutException.class);
    }

    private boolean isExceptionExpectedHostDown(Exception e) {
        return e.getClass().isAssignableFrom(ResourceAccessException.class) && e.getCause() != null
            && e.getCause().getClass().isAssignableFrom(HttpHostConnectException.class) && e.getCause().getCause() != null
            && e.getCause().getCause().getClass().isAssignableFrom(ConnectException.class);
    }

    private byte[] cameraReadPicture(Device device) {

        if (cameraUseMock()) {
            return createMockPicture();
        }

        try {
            String url = env.getProperty("camera." + deviceQualifier.idFrom(device) + ".url");
            ResponseEntity<byte[]> response = restTemplateBinaryResponse.getForEntity(url + "/capture", byte[].class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new IllegalStateException("could not capture picture from camera: " + response.getStatusCode());
            }
        } catch (RestClientException rce) {
            LOG.error("Error read camera picture: ", rce);
            return new byte[0];
        }
    }

    private boolean cameraUseMock() {
        return Boolean.parseBoolean(env.getProperty("camera.mock"));
    }

    private byte[] createMockPicture() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String text = sdf.format(new Date());

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = img.createGraphics();
        Font font = new Font("Arial", Font.PLAIN, 24);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text) + 2;
        int height = fm.getHeight() + 2;
        g2d.dispose();

        img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.BLUE);
        g2d.drawString(text, 0, fm.getAscent());
        g2d.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "jpg", out);
        } catch (IOException e) {
            LOG.error("Could not create mock image:", e);
        }
        return out.toByteArray();
    }

}
