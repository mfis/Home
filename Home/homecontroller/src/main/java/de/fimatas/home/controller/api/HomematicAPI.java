package de.fimatas.home.controller.api;

import static de.fimatas.home.controller.command.HomematicCommandConstants.E_O_F;
import static de.fimatas.home.controller.command.HomematicCommandConstants.PREFIX_RC;
import static de.fimatas.home.controller.command.HomematicCommandConstants.PREFIX_VAR;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.command.HomematicCommandConstants;
import de.fimatas.home.controller.command.HomematicCommandProcessor;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class HomematicAPI {

    @Autowired
    private Environment env;

    @Autowired
    @Qualifier("restTemplateCCU")
    private RestTemplate restTemplateCCU;

    @Autowired
    private HomematicCommandProcessor homematicCommandProcessor;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    private static final DateTimeFormatter UPTIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String host;

    private String ccuUser;

    private String ccuPass;

    private boolean writeToHomematicEnabled;

    private static final String REGA_HTTPS_PORT_AND_URI = ":48181/tclrega.exe";

    private static final String RESPONSE_ROOT_TAG = "xml";

    private static final String VAR_CCU_UPTIME = "CCU_Uptime";

    private static final String VAR_CCU_REBOOT = "CCU_im_Reboot";

    private static final int YEAR_OF_UNIX_TIMESTAMP_START = 1970;

    // current response

    private LocalDateTime currentValuesTimestamp;

    private String currentValuesCompareString;

    private Map<HomematicCommand, String> currentValues = new HashMap<>();

    //

    private boolean ccuInitState;

    private boolean isInitialDeviceStateSet = false;

    private long resourceNotAvailableCounter;

    private long lastCCUAuthTestTimestamp = 0;

    private Boolean ccuAuthActive;

    private DocumentBuilder documentBuilder;

    private static final Log LOG = LogFactory.getLog(HomematicAPI.class);

    @PostConstruct
    public void init() throws ParserConfigurationException {

        ccuUser = env.getProperty("homematic.authuser");
        ccuPass = env.getProperty("homematic.authpass");
        host = env.getProperty("homematic.hostName");
        String writeEnabled = env.getProperty("application.write.to.homematic");

        Assert.notNull(ccuUser, "ccuUser not set");
        Assert.notNull(ccuPass, "ccuPass not set");
        Assert.notNull(host, "host not set");
        Assert.notNull(writeEnabled, "writeEnabled not set");

        if (!host.startsWith("https://")) {
            throw new IllegalArgumentException("Non-SSL connections to ccu not allowed!");
        }

        writeToHomematicEnabled = Boolean.parseBoolean(writeEnabled.trim());

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance(); // NOSONAR
        dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        documentBuilder = dbFactory.newDocumentBuilder();
    }

    public boolean isDeviceUnreachableOrNotSending(Device device) {

        if (ccuInitState) {
            return true;
        }

        boolean unreachable = getAsBoolean(homematicCommandBuilder.read(device, Datapoint.UNREACH));
        if (unreachable) {
            return true;
        }

        Optional<Datapoint> datapointWithTimestamp =
            device.getDatapoints().stream().filter(Datapoint::isReadTimestamp).findFirst();

        if (datapointWithTimestamp.isPresent()) {
            LocalDateTime datapointTimestamp =
                getTimestamp(homematicCommandBuilder.readTS(device, datapointWithTimestamp.get()));
            if (datapointTimestamp.getYear() <= YEAR_OF_UNIX_TIMESTAMP_START) {
                return true;
            }
        }

        return false;
    }

    public String getAsString(HomematicCommand command) {
        return readValue(command);
    }

    public boolean getAsBoolean(HomematicCommand command) {
        return Boolean.valueOf(readValue(command));
    }

    public BigDecimal getAsBigDecimal(HomematicCommand command) {
        return new BigDecimal(readValue(command));
    }

    private LocalDateTime getTimestamp(HomematicCommand command) { // internal use only
        return LocalDateTime.parse(readValue(command), UPTIME_FORMATTER);
    }

    public void executeCommand(HomematicCommand... commands) {
        if (writeToHomematicEnabled) {
            executeCommands(CallType.WRITE, commands);
        } else {
            for (HomematicCommand command : commands) {
                LOG.info("Write to homematic is not enabled! - " + homematicCommandProcessor.buildCommand(command));
            }
        }
    }

    public boolean isPresent(HomematicCommand command) {
        return currentValues.containsKey(command);
    }

    private String readValue(HomematicCommand command) {
        if (currentValues.containsKey(command)) {
            return currentValues.get(command);
        } else {
            StringBuilder sb = new StringBuilder();
            for (HomematicCommand cmd : currentValues.keySet()) {
                sb.append(cmd.getCashedVarName() + "\n");
            }
            LOG.error("Key/Value unknown: " + command.getCashedVarName() + " / known keys: \n" + sb.toString());
            return null;
        }
    }

    private Boolean testCcuAuthIsActive(CallType callType) {

        if (callType == CallType.REFRESH && ccuAuthActive != null) {
            // no check when calling for refresh (should be as short as possible) except when no auth state is present
            return ccuAuthActive;
        }

        long now = System.currentTimeMillis();
        if (ccuAuthActive == null || (now - lastCCUAuthTestTimestamp) > 1000 * 60 * 60) { // 1h

            String body = buildReGaRequestBody(homematicCommandBuilder.eof());
            try {
                callReGaAPI(body, callType, false);
                ccuAuthActive = false;
            } catch (HttpClientErrorException e) {
                if (e.getRawStatusCode() == 401) {
                    ccuAuthActive = true;
                }
            } catch (Exception e) {
                ccuAuthActive = null;
            }

            if (ccuAuthActive != null) {
                lastCCUAuthTestTimestamp = now;
            }
        }
        return ccuAuthActive;
    }

    public synchronized boolean refresh() {

        long timeStart = System.nanoTime();

        if (!isInitialDeviceStateSet) {
            readDeviceState();
            if (!isInitialDeviceStateSet) {
                LOG.debug("skipped refresh due to unsuccsessful initial read of device state");
                return false;
            }
        }

        List<HomematicCommand> commands = new LinkedList<>();
        for (Device device : Device.values()) {
            for (Datapoint datapoint : device.getDatapoints()) {
                commands.add(homematicCommandBuilder.read(device, datapoint));
            }
            if (device.getSysVars() != null) {
                for (String suffix : device.getSysVars()) {
                    commands.add(homematicCommandBuilder.read(device, suffix));
                }
            }
        }

        boolean refreshed = executeCommands(CallType.REFRESH, commands.toArray(new HomematicCommand[commands.size()]));

        if (refreshed) {
            refreshed = hasChangedValues();
        }

        logRuntime("refresh", timeStart);
        return refreshed;
    }

    private boolean hasChangedValues() {

        String newCompareString = currentValues.toString();
        if (currentValuesCompareString != null && currentValuesCompareString.equals(newCompareString)) {
            if (ChronoUnit.SECONDS.between(currentValuesTimestamp,
                LocalDateTime.now()) < HomeAppConstants.MODEL_MAX_UPDATE_INTERVAL_SECONDS) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Response is equal to previous response AND model is still actual. -> NOT returning response.");
                }
                return false;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Response is equal to previous response BUT model is outdated. -> Returning response.");
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Response is NOT equal to previous response. -> Returning response.");
            }
        }
        currentValuesCompareString = newCompareString;
        return true;
    }

    @Scheduled(fixedDelay = (1000 * HomeAppConstants.DEVICE_STATE_INTERVAL_SECONDS))
    public synchronized void readDeviceState() {

        long timeStart = System.nanoTime();
        List<HomematicCommand> commands = new LinkedList<>();
        for (Device device : Device.values()) {
            for (Datapoint datapoint : device.getDatapoints()) {
                if (datapoint.isReadTimestamp()) {
                    commands.add(homematicCommandBuilder.readTS(device, datapoint));
                }
            }
            Datapoint lowBatDatapoint = device.lowBatDatapoint();
            if (lowBatDatapoint != null) {
                commands.add(homematicCommandBuilder.read(device, device.lowBatDatapoint()));
            }
            commands.add(homematicCommandBuilder.read(device, Datapoint.UNREACH));
        }
        commands.add(homematicCommandBuilder.read(VAR_CCU_REBOOT));
        commands.add(homematicCommandBuilder.read(VAR_CCU_UPTIME));

        if (executeCommands(CallType.DEVICE_STATE, commands.toArray(new HomematicCommand[commands.size()]))) {
            lookupInitState();
            logRuntime("readDeviceState", timeStart);
            isInitialDeviceStateSet = true;
        }
    }

    private void lookupInitState() {
        ccuInitState = getAsBoolean(homematicCommandBuilder.read(VAR_CCU_REBOOT));
    }

    private boolean executeCommands(CallType callType, HomematicCommand... commands) {

        testCcuAuthIsActive(callType);

        String body = buildReGaRequestBody(commands);
        return extractCommandResults(callType, callReGaAPI(body, callType, true), commands);
    }

    private boolean extractCommandResults(CallType callType, Document responseDocument, HomematicCommand... commands) {

        if (responseDocument == null) {
            LOG.debug("extractCommandResults recieved null for " + callType);
            return false;
        }

        Map<String, String> newStringToValues = new HashMap<>();
        boolean rcsOk = true;
        boolean eofOK = false;

        NodeList childs = responseDocument.getElementsByTagName(RESPONSE_ROOT_TAG).item(0).getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Element child = (Element) childs.item(i);
            if (child.getTagName().startsWith(PREFIX_VAR)) {
                extractVar(newStringToValues, child);
            } else if (child.getTagName().startsWith(PREFIX_RC)) {
                rcsOk = extractWriteExecRc(rcsOk, child);
            } else if (child.getTagName().equals(E_O_F)) {
                eofOK = extractEof(child);
            }
        }

        if (rcsOk && eofOK && (callType == CallType.REFRESH || callType == CallType.DEVICE_STATE)) {
            for (HomematicCommand command : commands) {
                currentValues.put(command, newStringToValues.get(command.getCashedVarName()));
            }
            if (callType == CallType.REFRESH) {
                currentValuesTimestamp = LocalDateTime.now();
            }
        }
        return rcsOk && eofOK;
    }

    private boolean extractEof(Element child) {

        boolean eofOK;
        eofOK = E_O_F.equals(child.getTextContent());
        if (!eofOK) {
            LOG.error("Command EOF not OK!");
        }
        return eofOK;
    }

    private boolean extractWriteExecRc(boolean rcsOk, Element child) {

        boolean rc = Boolean.parseBoolean(child.getTextContent());
        rcsOk = rcsOk && rc;
        if (!rc) {
            LOG.error("CommandResult not OK: " + child.getTagName());
        }
        return rcsOk;
    }

    private void extractVar(Map<String, String> newStringToValues, Element child) {

        newStringToValues.put(child.getTagName(), child.getTextContent());
    }

    private String buildReGaRequestBody(HomematicCommand... commands) {

        StringBuilder sb = new StringBuilder(commands.length * 120);
        boolean containsExecuteCommand = false;
        for (HomematicCommand command : commands) {
            if (command.isProgramRunCommand()) {
                containsExecuteCommand = true;
            }
            sb.append(homematicCommandProcessor.buildCommand(command));
            sb.append(" \n");
        }

        if (containsExecuteCommand) {
            sb.insert(0, homematicCommandProcessor
                .buildCommand(homematicCommandBuilder.write("refreshadress", env.getProperty("refresh.adress"))));
        }

        sb.append(homematicCommandProcessor.buildCommand(homematicCommandBuilder.eof()));
        return sb.toString();
    }

    private HttpHeaders createHeaders(int contentLength, boolean withAuthentication) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CACHE_CONTROL, "no-cache");
        if (withAuthentication) {
            httpHeaders.setBasicAuth(ccuUser, ccuPass, StandardCharsets.UTF_8);
        }
        httpHeaders.setAccept(Arrays.asList(MediaType.ALL));
        httpHeaders.setAcceptCharset(List.of(StandardCharsets.UTF_8));
        httpHeaders.setCacheControl(CacheControl.noCache());
        httpHeaders.setContentLength(contentLength);
        return httpHeaders;
    }

    private Document callReGaAPI(String body, CallType callType, boolean withAuthentication) {

        HttpHeaders headers = createHeaders(body.length(), withAuthentication);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplateCCU.postForEntity(host + REGA_HTTPS_PORT_AND_URI, requestEntity, String.class);
            resourceNotAvailableCounter = 0;
        } catch (Exception e) {
            if (handleRequestException(withAuthentication)) {
                throw e;
            }
            return null;
        }

        HttpStatus statusCode = responseEntity.getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            LOG.error("Could not successful call ReGa API. RC=" + statusCode.value());
        }
        Assert.notNull(responseEntity.getBody(), "ccu response is empty!");
        if (LOG.isTraceEnabled()) {
            LOG.trace("REQUEST:\n" + body + "\nRESPONSE:\n" + responseEntity.getBody());
        }

        try {
            Document newDoc = null;
            synchronized (documentBuilder) {
                newDoc =
                    documentBuilder.parse(new ByteArrayInputStream(responseEntity.getBody().getBytes(StandardCharsets.UTF_8))); // NOSONAR
            }
            newDoc.getDocumentElement().normalize();
            normalizeTimestamps(newDoc, callType);

            return newDoc;
        } catch (Exception e) {
            throw new IllegalStateException("Error parsing document", e);
        }
    }

    private void normalizeTimestamps(Document doc, CallType callType) {

        if (callType != CallType.DEVICE_STATE) {
            return;
        }

        NodeList childs = doc.getElementsByTagName(RESPONSE_ROOT_TAG).item(0).getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Element child = (Element) childs.item(i);
            if (child.getTagName().endsWith(HomematicCommandConstants.SUFFIX_TS)) {
                String ts = child.getTextContent();
                if (StringUtils.isNotBlank(ts)) {
                    LocalDateTime localDateTime = LocalDateTime.parse(ts, UPTIME_FORMATTER);
                    LocalDateTime normalizedLocalDateTime = localDateTime.truncatedTo(ChronoUnit.HOURS);
                    child.setTextContent(normalizedLocalDateTime.format(UPTIME_FORMATTER));
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Normalized Timestamp: " + localDateTime + " -> " + normalizedLocalDateTime);
                    }
                }
            }
        }
    }

    public boolean handleRequestException(boolean withAuthentication) {

        if (!withAuthentication) {
            return true;
        }
        resourceNotAvailableCounter++;
        return resourceNotAvailableCounter > 2;
    }

    public LocalDateTime getCurrentValuesTimestamp() {
        return currentValuesTimestamp;
    }

    public Boolean getCcuAuthActive() {
        return ccuAuthActive;
    }

    private void logRuntime(String cpt, long start) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(cpt + "=" + ((System.nanoTime() - start) / 1000000) + " ms");
        }
    }

    private enum CallType {
        REFRESH, DEVICE_STATE, INDIVIDUAL_READ, WRITE
    }
}
