package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.NoticeDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.Notice;
import de.fimatas.home.library.model.NoticeModel;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@CommonsLog
public class NoticesService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private NoticeDAO noticeDAO;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Scheduled(initialDelay = 3000, fixedDelay = 1000 * 60 * 15)
    public void scheduledRefresh() {
        refresh();
    }

    public void refresh() {

        var notices = noticeDAO.getLatestNotices();
        notices.forEach(this::lookupDerivedTitle);
        var model = new NoticeModel();
        model.setNotices(notices);

        // TODO: read history versions

        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

    public void createNew(Message message) {

        var uuid = UUID.randomUUID().toString();
        long version = noticeDAO.createNew(uuid, message.getUser(), "");
        message.setDeviceId(uuid);
        message.setKey(Long.toString(version));
        message.setValue("");

        refresh();
    }

    public synchronized void save(Message message) {

        if(ModelObjectDAO.getInstance().readNoticeModel() == null) {
            throw new IllegalStateException("Model not found");
        }

        var notice = ModelObjectDAO.getInstance().readNoticeModel().getNotices().stream()
                .filter(n -> n.getId().equals(message.getDeviceId())).findFirst().orElse(null);
        if(notice == null) {
            throw new IllegalStateException("Message not found");
        }

        // FIXME: CHECK VERSION

        noticeDAO.modify(message.getDeviceId(), message.getUser(), message.getValue());

        refresh();
    }

    private void lookupDerivedTitle(Notice notice) {

        if(notice == null) {
            return;
        }

        if(StringUtils.isBlank(notice.getText())){
            notice.setDerivedTitle("[ Leer ]");
            return;
        }

        String firstLine = StringUtils.trimToEmpty(notice.getText()).split("\\R", 2)[0].trim();
        notice.setDerivedTitle(firstLine
                .replaceAll("^#+\\s*", "")
                .replaceAll("[*_~`]", "")
                .trim());
    }

}
