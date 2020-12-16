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

import de.hybris.platform.cloud.commons.constants.CloudCommonsConstants;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;

import java.util.function.Consumer;

import com.codahale.metrics.Counter;

/**
 * @inheritDoc
 * Records {@link MonitorHistoryData} as Metric Registry Counter data
 */
public class CounterMetricRecorder extends AbstractMetricRecorder
{
	private static final String METRIC_TYPE = "counter";

	@Override
	protected String getEnabledKey()
	{
		return CloudCommonsConstants.MONITORING_METRIC_COUNTER_ENABLED;
	}

	@Override
	protected void processHistory(final MonitorHistoryData history)
	{
		final String key = getAreaMetricName(history.getSystemArea(), history.getStatus().name(), METRIC_TYPE);
		adjustMetric(key, Counter::inc);

		if (history.getStatusWhenResumed() != null)
		{
			final String checkpointKey = getAreaMetricName(history.getSystemArea(), history.getStatusWhenResumed().name(), METRIC_TYPE);
			adjustMetric(checkpointKey, Counter::dec);
		}
	}

	@Override
	protected void processHistoryEntry(final MonitorHistoryData history, final MonitorHistoryEntryData entry)
	{
		final String key = getStepMetricName(history.getSystemArea(), entry.getStep(), entry.getStatus().name(), METRIC_TYPE);
		adjustMetric(key, Counter::inc);
	}

	protected void adjustMetric(final String key, final Consumer<Counter> apply)
	{
		final Counter counter = getMetricRegistry().counter(key);
		apply.accept(counter);
	}
}
