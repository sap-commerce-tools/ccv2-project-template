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
package de.hybris.platform.cloud.commons.services.monitor.logging;

import de.hybris.platform.cloud.commons.constants.CloudCommonsConstants;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.impl.AbstractConfigEnabledMonitorRecorder;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

/**
 * @inheritDoc
 * Record {@link MonitorHistoryData} into Log files
 */
public class LoggingMonitorRecorder extends AbstractConfigEnabledMonitorRecorder
{
	private Converter<MonitorHistoryData, LogData> historyConverter;
	private List<Status> infoStatuses;
	private List<Status> warnStatuses;
	private List<Status> errorStatuses;

	@Override
	protected String getEnabledKey()
	{
		return CloudCommonsConstants.MONITORING_LOGGING_ENABLED;
	}

	@Override
	protected void recordHistory(final MonitorHistoryData history)
	{
		final LogData logData = getHistoryConverter().convert(history);

		final Status status = history.getStatus();

		if (getInfoStatuses().contains(status))
		{
			log.info(logData.getProcessedMessage());
		}
		else if (getWarnStatuses().contains(status))
		{
			log.warn(logData.getProcessedMessage());
		}
		else if (getErrorStatuses().contains(status))
		{
			if (logData.getException() != null)
			{
				log.error(logData.getProcessedMessage(), logData.getException());
			}
			else
			{
				log.error(logData.getProcessedMessage());
			}
		}
	}

	protected Converter<MonitorHistoryData, LogData> getHistoryConverter()
	{
		return historyConverter;
	}

	@Required
	public void setHistoryConverter(final Converter<MonitorHistoryData, LogData> historyConverter)
	{
		this.historyConverter = historyConverter;
	}

	protected List<Status> getInfoStatuses()
	{
		return infoStatuses;
	}

	@Required
	public void setInfoStatuses(final List<Status> infoStatuses)
	{
		this.infoStatuses = infoStatuses;
	}

	protected List<Status> getErrorStatuses()
	{
		return errorStatuses;
	}

	@Required
	public void setErrorStatuses(final List<Status> errorStatuses)
	{
		this.errorStatuses = errorStatuses;
	}

	protected List<Status> getWarnStatuses()
	{
		return warnStatuses;
	}

	@Required
	public void setWarnStatuses(final List<Status> warnStatuses)
	{
		this.warnStatuses = warnStatuses;
	}
}
