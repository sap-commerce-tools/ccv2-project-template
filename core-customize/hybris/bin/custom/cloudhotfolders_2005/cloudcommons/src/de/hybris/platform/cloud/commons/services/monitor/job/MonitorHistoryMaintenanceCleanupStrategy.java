/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company. All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.cloud.commons.services.monitor.job;

import de.hybris.platform.cloud.commons.enums.MonitorStatus;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.jobs.maintenance.MaintenanceCleanupStrategy;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.time.TimeService;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * A {@link MaintenanceCleanupStrategy} implementation to locate and process {@link MonitorHistoryDataModel}'s.
 */
public class MonitorHistoryMaintenanceCleanupStrategy implements MaintenanceCleanupStrategy<MonitorHistoryDataModel, CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorHistoryMaintenanceCleanupStrategy.class);
    private static final int DEFAULT_THRESHOLD_DAYS = 7;
    private static final String TEMPLATE = "SELECT {%s} FROM {%s} WHERE {%s} < ?threshold AND {%s} %s ?status";
    protected static final String STATUS_PARAM = "status";
    protected static final String THRESHOLD_PARAM = "threshold";

    private ConfigurationService configurationService;
    private ModelService modelService;
    private TimeService timeService;
    private MonitorStatus status;
    private String statusCondition;
    private String thresholdConfigKey;

    @Override
    public FlexibleSearchQuery createFetchQuery(final CronJobModel cronJob) {

        final HashMap<String, Object> params = new HashMap<>();
        params.put(MonitorHistoryMaintenanceCleanupStrategy.STATUS_PARAM, this.getStatus());
        params.put(MonitorHistoryMaintenanceCleanupStrategy.THRESHOLD_PARAM, this.calculateThreshold());

        final String queryString = String.format(TEMPLATE,
                MonitorHistoryDataModel.PK,
                MonitorHistoryDataModel._TYPECODE,
                MonitorHistoryDataModel.MODIFIEDTIME,
                MonitorHistoryDataModel.STATUS,
                this.getStatusCondition()
        );

        return new FlexibleSearchQuery(queryString, params);
    }

    protected Date calculateThreshold() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.getTimeService().getCurrentTime());
        int thresholdDays = this.getThresholdDays();
        calendar.add(Calendar.DATE, -thresholdDays);
        return calendar.getTime();
    }

    protected int getThresholdDays() {
        final ConfigurationService configService = this.getConfigurationService();
        final Configuration config = configService.getConfiguration();
        return config.getInt(this.getThresholdConfigKey(), MonitorHistoryMaintenanceCleanupStrategy.DEFAULT_THRESHOLD_DAYS);
    }

    @Override
    public void process(final List<MonitorHistoryDataModel> elements) {
        LOG.info("Removing " + elements.size());
        this.getModelService().removeAll(elements);
    }

    protected ConfigurationService getConfigurationService() {
        return this.configurationService;
    }

    @Required
    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected ModelService getModelService() {
        return this.modelService;
    }

    @Required
    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    protected TimeService getTimeService() {
        return this.timeService;
    }

    @Required
    public void setTimeService(final TimeService timeService) {
        this.timeService = timeService;
    }

    protected MonitorStatus getStatus() {
        return this.status;
    }

    @Required
    public void setStatus(final MonitorStatus status) {
        this.status = status;
    }

    protected String getStatusCondition() {
        return this.statusCondition;
    }

    @Required
    public void setStatusCondition(final String statusCondition) {
        this.statusCondition = statusCondition;
    }

    protected String getThresholdConfigKey() {
        return this.thresholdConfigKey;
    }

    @Required
    public void setThresholdConfigKey(final String thresholdConfigKey) {
        this.thresholdConfigKey = thresholdConfigKey;
    }
}
