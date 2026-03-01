package de.fimatas.home.client.service;

import de.fimatas.home.client.model.MessageQueue;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.MessageType;
import de.fimatas.users.logic.UserEventAdapter;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class ControllerPushMessageService {

    @PostConstruct
    public void init() {
        UserEventAdapter.setOnMessage(msg -> {
            log.warn(msg);
            Message message = new Message();
            message.setMessageType(MessageType.CLIENT_ERROR_PUSH_MESSAGE);
            message.setValue(msg);
            MessageQueue.getInstance().request(message, false);
        });
    }
}
