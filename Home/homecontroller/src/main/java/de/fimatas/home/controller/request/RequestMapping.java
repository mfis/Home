package de.fimatas.home.controller.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.domain.model.ActionModel;

@RestController
public class RequestMapping {

    @Autowired
    private HouseService houseService;

    @GetMapping("/controller/refresh")
    public ActionModel refresh() {
        houseService.refreshHouseModel(false);
        return new ActionModel("OK");
    }

}