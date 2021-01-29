package de.fimatas.home.client.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class SettingsViewService {

    @Value("${pushtoken.acceptNotAvailableToken}")
    private boolean acceptNotAvailableToken;

    public boolean isValidPushToken(String pushToken) {

        if (StringUtils.isBlank(pushToken)) {
            return false;
        }

        if (HomeAppConstants.PUSH_TOKEN_NOT_AVAILABLE_INDICATOR.equals(pushToken)) {
            return acceptNotAvailableToken;
        } else {
            return true;
        }
    }

    public String allSettingsAsString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(ModelObjectDAO.getInstance().readAllSettings());
        } catch (JsonProcessingException e) {
            return StringUtils.EMPTY;
        }
    }
}
