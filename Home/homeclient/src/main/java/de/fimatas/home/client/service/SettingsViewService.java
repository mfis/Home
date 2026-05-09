package de.fimatas.home.client.service;

import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.util.HomeAppConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SettingsViewService {

    @Value("${pushtoken.acceptNotAvailableToken}")
    private boolean acceptNotAvailableToken;

    @Autowired
    private JsonMapper jsonMapper;

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
            return jsonMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(ModelObjectDAO.getInstance().readAllSettings());
        } catch (JacksonException e) {
            return StringUtils.EMPTY;
        }
    }
}
