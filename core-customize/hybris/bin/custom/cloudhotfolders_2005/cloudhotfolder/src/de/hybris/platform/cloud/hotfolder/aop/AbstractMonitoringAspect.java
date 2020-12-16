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
package de.hybris.platform.cloud.hotfolder.aop;

import de.hybris.platform.cloud.commons.services.monitor.*;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.Date;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public class AbstractMonitoringAspect
{
	private static final Logger LOG = getLogger(BatchHeaderAspect.class);

	private MonitorService monitorService;

	protected void beginMonitor(final SystemArea area, final String key)
	{
		getMonitorService().begin(area, key);
	}

	protected void resumeMonitor(final SystemArea area, final String key)
	{
		getMonitorService().resume(area, key);
	}

	protected void monitorSuccessfulStep(final Step step, final Date started, final String message, final Object... messageArgs)
	{
		final Optional<MonitorHistory> history = getMonitorService().current();
		if (!history.isPresent())
		{
			LOG.warn("No MonitorHistory within the thread, cannot add successful Step [{}] started [{}]", step, started);
		}
		else
		{
			history.get().stepSucceeded(step, started, new Date(), message, messageArgs);
		}
	}

	protected void monitorFailedStep(final Step step, final Date started, final Throwable e, final String message, final Object... messageArgs)
	{
		final Optional<MonitorHistory> history = getMonitorService().current();
		if (!history.isPresent())
		{
			LOG.warn("No MonitorHistory within the thread, cannot add failed Step [{}] started [{}]", step, started);
		}
		else
		{
			history.get().stepFailed(step, started, new Date(), e, message, messageArgs);
		}
	}

	protected void monitorAction(final String code, final Status status, final Date started, final String message, final Object... messageArgs)
	{
		final Optional<MonitorHistory> history = getMonitorService().current();
		if (!history.isPresent())
		{
			LOG.warn("No MonitorHistory within the thread, cannot add action Code [{}] Status [{}] started [{}]", code, status, started);
		}
		else
		{
			history.get().addAction(code, status, started, new Date(), message, messageArgs);
		}
	}

	protected void checkpointMonitor()
	{
		final Optional<MonitorHistory> history = getMonitorService().current();
		if (!history.isPresent())
		{
			LOG.warn("No MonitorHistory within the thread, cannot checkpoint");
		}
		else
		{
			history.get().checkpoint();
		}
	}

	protected void endMonitor(final Status status)
	{
		final Optional<MonitorHistory> history = getMonitorService().current();
		if (!history.isPresent())
		{
			LOG.warn("No MonitorHistory within the thread, cannot end with Status [{}]", status);
		}
		else
		{
			history.get().end(status);
		}
	}

	protected MonitorService getMonitorService()
	{
		return monitorService;
	}

	@Required
	public void setMonitorService(final MonitorService monitorService)
	{
		this.monitorService = monitorService;
	}
}
