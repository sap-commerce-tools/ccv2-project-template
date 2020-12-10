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
package de.hybris.platform.cloud.commons.services.monitor.impl;

import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorRecorder;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * @inheritDoc An abstract {@link MonitorRecorder} strategy to control a concrete implementation via a configuration property
 */
public abstract class AbstractConfigEnabledMonitorRecorder implements MonitorRecorder
{
    // Suppress Sonar warnings - Protected non static logger so that
    // subclasses can access a single, consistent logger with a fixed name.
    // Provides better log consistency for this use case.
    @SuppressWarnings({"squid:S1312"})
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private ConfigurationService configurationService;

	@Override
	public void record(final MonitorHistoryData history)
	{
		if (isEnabled())
		{
			recordHistory(history);
		}
        else if (log.isDebugEnabled())
		{
            log.debug("[{}] is not enabled, will not record for Area [{}] Key [{}]", getEnabledKey(), history.getSystemArea(),
					history.getKey());
		}

	}

	protected boolean isEnabled()
	{
		return getConfigurationService().getConfiguration().getBoolean(getEnabledKey(), false);
	}

	protected abstract String getEnabledKey();

	protected abstract void recordHistory(final MonitorHistoryData history);

	protected ConfigurationService getConfigurationService()
	{
		return configurationService;
	}

	@Required
	public void setConfigurationService(final ConfigurationService configurationService)
	{
		this.configurationService = configurationService;
	}
}
