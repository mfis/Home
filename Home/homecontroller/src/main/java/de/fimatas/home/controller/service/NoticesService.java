package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.Notice;
import de.fimatas.home.library.model.NoticeModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@CommonsLog
public class NoticesService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    private NoticeModel instanceModel = null;

    @Scheduled(initialDelay = 5000, fixedDelay = 1000 * 60 * 15)
    public void scheduledRefresh() {
        refresh();
    }

    @PostConstruct
    public void init() {
        var noticeModel = new NoticeModel();
        var n1 = new Notice();
        n1.setId("id1");
        n1.setDerivedTitle("Title 1");
        n1.setUser("test");
        n1.setLastEdited(LocalDateTime.now());
        n1.setText("""
                # Überschrift
                
                <!-- COMMENT -->
                | COL 1    | COL 2 |
                | -------- | ------- |
                | A  | 1    |
                | B | 2     |
                | B    | 3    |
                """);
        var n2 = new Notice();
        n2.setId("id2");
        n2.setDerivedTitle("Title 2");
        n2.setUser(null);
        n2.setLastEdited(LocalDateTime.now().minusDays(3));
        n2.setText("Text 2");
        noticeModel.getNotices().add(n1);
        noticeModel.getNotices().add(n2);
        instanceModel =  noticeModel;
    }

    public void refresh() {

        ModelObjectDAO.getInstance().write(instanceModel);
        uploadService.uploadToClient(instanceModel);
    }

    public void save(Message message) {

        var notice = instanceModel.getNotices().stream().filter(n -> n.getId().equals(message.getDeviceId())).findFirst().orElse(null);
        if(notice == null) {
            throw new IllegalStateException("Message not found");
        }
        notice.setVersion(notice.getVersion() + 1);
        notice.setUser(Boolean.parseBoolean(message.getAdditionalData()) ? null : message.getUser());
        notice.setText(message.getValue());
        message.setKey(Long.toString(notice.getVersion()));
        refresh();
    }


}
