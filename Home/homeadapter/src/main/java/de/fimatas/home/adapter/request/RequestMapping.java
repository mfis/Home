package de.fimatas.home.adapter.request;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import de.fimatas.home.adapter.auth.HomekitAuthentication;
import de.fimatas.home.library.domain.model.ActionModel;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
public class RequestMapping {

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