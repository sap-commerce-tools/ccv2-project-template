package de.hybris.platform.hac.controller;

import mpern.sap.commerce.ribbon.EnvironmentMetaData;
import mpern.sap.commerce.ribbon.EnvironmentMetaDataService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class RibbonDataController {

    @Resource
    private EnvironmentMetaDataService environmentMetaDataService;

    @RequestMapping(value = "/ribbon/data", method = RequestMethod.GET)
    public EnvironmentMetaData getRibbonData() {
        return environmentMetaDataService.getMetaData();
    }
}
