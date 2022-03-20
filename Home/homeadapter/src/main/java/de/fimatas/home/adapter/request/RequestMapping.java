package de.fimatas.home.adapter.request;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import de.fimatas.home.adapter.auth.HomekitAuthentication;
import de.fimatas.home.adapter.service.HomekitService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.ActionModel;
import de.fimatas.home.library.domain.model.HouseModel;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@CommonsLog
public class RequestMapping {

    @Autowired
    private HomekitService homekitService;

    private static final Log LOG = LogFactory.getLog(RequestMapping.class);

    @PostMapping(value = "/uploadHouseModel")
    public ActionModel uploadHouseModel(@RequestBody HouseModel houseModel) {
        ModelObjectDAO.getInstance().write(houseModel);
        try {
            homekitService.update();
        }catch (Exception e){
            LOG.error("Exception updating HomekitService", e);
        }
        return new ActionModel("OK");
    }

    @GetMapping("adapter/memoryInfo")
    public ActionModel memoryInfo() {
        final Runtime runtime = Runtime.getRuntime();
        final long MEM_FACTOR_MB = 1024L * 1024L;
        final String MB = "MB";
        String info = "Memory Information:" +
                " free:" + (runtime.freeMemory() / MEM_FACTOR_MB) + MB +
                " allocated:" + (runtime.totalMemory() / MEM_FACTOR_MB) + MB +
                " max:" + (runtime.maxMemory() / MEM_FACTOR_MB) + MB +
                " totalFree:" + (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory())) / MEM_FACTOR_MB + MB;
        return new ActionModel(info);
    }

    @GetMapping(value = "/adapter/pairing", produces = "image/png")
    public ResponseEntity<byte[]> homekit() throws WriterException, IOException {

        log.info("Setup URL: " + HomekitAuthentication.getInstance().getSetupURI());

        BitMatrix matrix = new MultiFormatWriter().encode(
                HomekitAuthentication.getInstance().getSetupURI(), BarcodeFormat.QR_CODE, 300,
                300);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "png", baos);
        byte[] bytes = baos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("image", "png"));
        headers.setContentDispositionFormData("attachment", "homekit.png");
        headers.setContentLength(bytes.length);
        headers.setCacheControl(CacheControl.noCache());

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}