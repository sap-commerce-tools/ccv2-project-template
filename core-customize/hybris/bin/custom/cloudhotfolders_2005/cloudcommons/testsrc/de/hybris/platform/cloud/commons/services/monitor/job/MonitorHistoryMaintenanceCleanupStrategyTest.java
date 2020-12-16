package de.hybris.platform.cloud.commons.services.monitor.job;

import com.google.common.collect.ImmutableMap;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.enums.MonitorStatus;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.time.TimeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@UnitTest
public class MonitorHistoryMaintenanceCleanupStrategyTest {

    private MonitorHistoryMaintenanceCleanupStrategy strategy = new MonitorHistoryMaintenanceCleanupStrategy();

    @Mock
    private ModelService modelService;

    @Mock
    private CronJobModel cronJob;

    @Mock
    private TimeService timeService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConfigurationService configurationService;

    private final Date seedDate = new Date();

    @Before
    public void setUp() {
        strategy.setConfigurationService(configurationService);
        strategy.setModelService(modelService);
        strategy.setTimeService(timeService);
        strategy.setThresholdConfigKey("config.key");

        given(timeService.getCurrentTime()).willReturn(seedDate);
        given(configurationService.getConfiguration().getInt("config.key", 7)).willReturn(10);

    }

    @Test
    public void createQueryShouldReturnEqualToWhenRequired() {
        strategy.setStatus(MonitorStatus.SUCCESS);
        strategy.setStatusCondition("=");

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(seedDate);
        calendar.add(Calendar.DATE, -10);

        final FlexibleSearchQuery query = strategy.createFetchQuery(cronJob);

        assertThat(query.getQuery()).isEqualTo("SELECT {pk} FROM {MonitorHistoryData} WHERE {modifiedtime} < ?threshold AND {status} = ?status");
        assertThat(query.getQueryParameters()).isEqualTo(
                ImmutableMap.builder()
                        .put(MonitorHistoryMaintenanceCleanupStrategy.THRESHOLD_PARAM, calendar.getTime())
                        .put(MonitorHistoryMaintenanceCleanupStrategy.STATUS_PARAM, MonitorStatus.SUCCESS)
                        .build());
    }

    @Test
    public void createQueryShouldReturnNotEqualToWhenRequired() {
        strategy.setStatus(MonitorStatus.SUCCESS);
        strategy.setStatusCondition("!=");

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(seedDate);
        calendar.add(Calendar.DATE, -10);

        final FlexibleSearchQuery query = strategy.createFetchQuery(cronJob);

        assertThat(query.getQuery()).isEqualTo("SELECT {pk} FROM {MonitorHistoryData} WHERE {modifiedtime} < ?threshold AND {status} != ?status");
        assertThat(query.getQueryParameters()).isEqualTo(ImmutableMap.builder()
                .put(MonitorHistoryMaintenanceCleanupStrategy.THRESHOLD_PARAM, calendar.getTime())
                .put(MonitorHistoryMaintenanceCleanupStrategy.STATUS_PARAM, MonitorStatus.SUCCESS)
                .build());
    }

    @Test
    public void processShouldRemoveAllModels() {
        final MonitorHistoryDataModel model = mock(MonitorHistoryDataModel.class);

        strategy.process(Collections.singletonList(model));

        verify(modelService).removeAll(Collections.singletonList(model));
    }

}