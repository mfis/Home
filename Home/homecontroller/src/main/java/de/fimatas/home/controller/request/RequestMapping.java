package de.fimatas.home.controller.request;

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.domain.model.ActionModel;

@RestController
public class RequestMapping {

    @Autowired
    private HouseService houseService;

    private static final Log LOG = LogFactory.getLog(RequestMapping.class);

    @PostConstruct
    private void postConstruct() {
        String uuid = UUID.randomUUID().toString();
        LOG.info("UUID=" + uuid);
    }

    @GetMapping("/controller/refresh")
    public ActionModel refresh() {
        houseService.refreshHouseModel();
        return new ActionModel("OK");
    }

}