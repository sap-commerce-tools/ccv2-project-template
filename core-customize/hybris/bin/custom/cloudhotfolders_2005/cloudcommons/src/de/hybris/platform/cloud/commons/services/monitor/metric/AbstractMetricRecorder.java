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
package de.hybris.platform.cloud.commons.services.monitor.metric;

import com.codahale.metrics.MetricRegistry;

import de.hybris.platform.cloud.commons.constants.CloudCommonsConstants;
import de.hybris.platform.cloud.commons.services.monitor.*;
import de.hybris.platform.cloud.commons.services.monitor.impl.AbstractConfigEnabledMonitorRecorder;
import de.hybris.platform.core.Registry;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Required;

import java.text.MessageFormat;
import java.util.Objects;

public abstract class AbstractMetricRecorder extends AbstractConfigEnabledMonitorRecorder
{
	private static final String AREA_METRIC_KEY_TEMPLATE = String.format("tenant={0},extension=%s,area={1},name={2}", CloudCommonsConstants.EXTENSIONNAME);
	private static final String STEP_METRIC_KEY_TEMPLATE = String.format("tenant={0},extension=%s,area={1},step={2},name={3}", CloudCommonsConstants.EXTENSIONNAME);

	private MetricRegistry metricRegistry;
	private MonitorEntryFilter entryFilter;

	@Override
	protected void recordHistory(final MonitorHistoryData history)
	{
		processHistory(history);
		if (CollectionUtils.isNotEmpty(history.getEntries()))
		{
			final MonitorEntryFilter filter = getEntryFilter();
			history.getEntries().stream()
					.filter(Objects::nonNull)
					.filter(filter::apply)
					.forEach(e -> processHistoryEntry(history, e));
		}
	}

	protected abstract void processHistory(final MonitorHistoryData history);

	protected abstract void processHistoryEntry(final MonitorHistoryData history, final MonitorHistoryEntryData entry);

	protected String getAreaMetricName(final SystemArea module, final String name, final String... names)
	{
		return MessageFormat.format(AREA_METRIC_KEY_TEMPLATE, getTenantId(), module.name(), MetricRegistry.name(name, names));
	}

	protected String getStepMetricName(final SystemArea module, final Step step, final String name, final String... names)
	{
		return MessageFormat.format(STEP_METRIC_KEY_TEMPLATE, getTenantId(), module.name(), step.name(), MetricRegistry.name(name, names));
	}

	protected Long getDuration(final SystemArea area, final MonitorHistoryEntryData entryData)
	{
		if (entryData.getDuration() != null)
		{
			return entryData.getDuration();
		}

		if (entryData.getStarted() != null && entryData.getEnded() != null)
		{
			return entryData.getEnded().getTime() - entryData.getStarted().getTime();
		}

		log.debug("entry for Area [{}] Step [{}] Status [{}] had no duration or start/end times", area, entryData.getStep(), entryData.getStatus());
		return null;
	}

	protected String getTenantId()
	{
		return Registry.getCurrentTenant().getTenantID();
	}

	protected MetricRegistry getMetricRegistry()
	{
		return metricRegistry;
	}

	@Required
	public void setMetricRegistry(final MetricRegistry metricRegistry)
	{
		this.metricRegistry = metricRegistry;
	}


	protected MonitorEntryFilter getEntryFilter()
	{
		return entryFilter;
	}

	@Required
	public void setEntryFilter(final MonitorEntryFilter entryFilter)
	{
		this.entryFilter = entryFilter;
	}
}
