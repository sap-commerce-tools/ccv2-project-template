package de.hybris.platform.modelt.readinesscheck.impl;

import de.hybris.platform.modelt.readinesscheck.ReadinessCheck;
import de.hybris.platform.modelt.readinesscheck.ReadinessCheckResult;
import de.hybris.platform.modelt.readinesscheck.ReadinessCheckService;
import de.hybris.platform.modelt.readinesscheck.ReadinessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class DefaultReadinessCheckService implements ReadinessCheckService {

    private List<ReadinessCheck> readinessChecks;

    @Override
    public ReadinessCheckResult checkReadiness() {
        if (CollectionUtils.isEmpty(readinessChecks)) {
            return ReadinessCheckResult.ready();
        }

        for (ReadinessCheck readinessCheck : readinessChecks) {
            if (ReadinessStatus.READY != readinessCheck.getResult().getReadinessStatus()) {
                return readinessCheck.getResult();
            }
        }

        return ReadinessCheckResult.ready();
    }

    @Required
    public void setReadinessChecks(final List<ReadinessCheck> readinessChecks) {
        this.readinessChecks = readinessChecks;
    }
}