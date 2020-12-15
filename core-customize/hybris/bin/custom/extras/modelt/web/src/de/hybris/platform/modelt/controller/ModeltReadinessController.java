package de.hybris.platform.modelt.controller;

import de.hybris.platform.modelt.readinesscheck.ReadinessCheckResult;
import de.hybris.platform.modelt.readinesscheck.ReadinessCheckService;
import de.hybris.platform.modelt.readinesscheck.ReadinessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModeltReadinessController {

    @Autowired
    @Qualifier("readinessCheckService")
    private ReadinessCheckService readinessCheckService;

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<String> isReady() {
        ReadinessCheckResult readinessCheckResult = readinessCheckService.checkReadiness();
        if (ReadinessStatus.READY == readinessCheckResult.getReadinessStatus()) {
            return ResponseEntity.ok("ready");
        }
        return ResponseEntity.status(readinessCheckResult.getHttpStatus()).body("not ready yet");
    }
}
