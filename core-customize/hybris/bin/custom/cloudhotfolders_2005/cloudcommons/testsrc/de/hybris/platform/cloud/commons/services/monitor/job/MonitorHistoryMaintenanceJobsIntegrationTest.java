package de.hybris.platform.cloud.commons.services.monitor.job;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.cloud.commons.enums.MonitorStatus;
import de.hybris.platform.cloud.commons.enums.MonitorSystemArea;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.core.PK;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.jobs.GenericMaintenanceJobPerformable;
import de.hybris.platform.servicelayer.ServicelayerTransactionalBaseTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.exceptions.ModelLoadingException;
import de.hybris.platform.servicelayer.internal.model.MaintenanceCleanupJobModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.time.TimeService;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@IntegrationTest
public class MonitorHistoryMaintenanceJobsIntegrationTest extends ServicelayerTransactionalBaseTest {

    @Resource
    private ModelService modelService;

    @Resource
    private TimeService timeService;

    @Resource
    private GenericMaintenanceJobPerformable cleanUpMonitorHistorySuccessfulJob;

    @Resource
    private GenericMaintenanceJobPerformable cleanUpMonitorHistoryUnsuccessfulJob;

    @Resource
    private ConfigurationService configurationService;

    private int successfulThreshold;
    private int unsuccessfulThreshold;

    @Before
    public void setUp() {
        final Configuration configuration = configurationService.getConfiguration();
        successfulThreshold = configuration.getInt("cloud.commons.monitoring.job.cleanup.successful.threshold");
        unsuccessfulThreshold = configuration.getInt("cloud.commons.monitoring.job.cleanup.unsuccessful.threshold");
    }

    @After
    public void tearDown() {
        //Go back to current date/time
        timeService.setCurrentTime(null);
    }

    @Test
    public void shouldDeleteSuccessfulDataOnceThresholdIsReached() {

        //Go back to current date/time
        timeService.setCurrentTime(null);

        final MonitorHistoryDataModel dataModel = modelService.create(MonitorHistoryDataModel.class);
        dataModel.setKey("success");
        dataModel.setStatus(MonitorStatus.SUCCESS);
        dataModel.setSystemArea(MonitorSystemArea.INTEGRATION);

        modelService.save(dataModel);
        final long pk = dataModel.getPk().getLong();

        final Date thresholdDate = new DateTime(dataModel.getModifiedtime())
                .plusDays(successfulThreshold)
                .plusSeconds(1)
                .toDate();

        runJob(cleanUpMonitorHistorySuccessfulJob);

        assertNotNull("PK should still exist", getModelForPk(pk));

        timeService.setCurrentTime(thresholdDate);
        runJob(cleanUpMonitorHistorySuccessfulJob);

        assertNull("PK should not exist", getModelForPk(pk));
    }

    @Test
    public void shouldDeleteUnsuccessfulDataOnceThresholdIsReached() {

        //Go back to current date/time
        timeService.setCurrentTime(null);

        final MonitorHistoryDataModel dataModel = modelService.create(MonitorHistoryDataModel.class);
        dataModel.setKey("failure");
        dataModel.setStatus(MonitorStatus.FAILURE);
        dataModel.setSystemArea(MonitorSystemArea.INTEGRATION);

        modelService.save(dataModel);
        final long pk = dataModel.getPk().getLong();
        final Date successfulThresholdDate = new DateTime(dataModel.getModifiedtime())
                .plusDays(successfulThreshold)
                .plusSeconds(1)
                .toDate();
        final Date unsuccessfulThresholdDate = new DateTime(dataModel.getModifiedtime())
                .plusDays(unsuccessfulThreshold)
                .plusSeconds(1)
                .toDate();

        runJob(cleanUpMonitorHistoryUnsuccessfulJob);

        assertNotNull("PK should still exist", getModelForPk(pk));

        timeService.setCurrentTime(successfulThresholdDate);
        runJob(cleanUpMonitorHistoryUnsuccessfulJob);

        assertNotNull("PK should still exist", getModelForPk(pk));

        timeService.setCurrentTime(unsuccessfulThresholdDate);
        runJob(cleanUpMonitorHistoryUnsuccessfulJob);

        assertNull("PK should not exist", getModelForPk(pk));
    }

    private void runJob(final GenericMaintenanceJobPerformable performable) {
        final MaintenanceCleanupJobModel job = new MaintenanceCleanupJobModel();
        final CronJobModel cjm = new CronJobModel();
        cjm.setJob(job);
        final PerformResult result = performable.perform(cjm);
        assertEquals(CronJobResult.SUCCESS, result.getResult());
        assertEquals(CronJobStatus.FINISHED, result.getStatus());
    }

    private MonitorHistoryDataModel getModelForPk(final long pk) {
        try {
            return modelService.get(PK.fromLong(pk));
        } catch (final ModelLoadingException e) {
            return null;
        }
    }

}
