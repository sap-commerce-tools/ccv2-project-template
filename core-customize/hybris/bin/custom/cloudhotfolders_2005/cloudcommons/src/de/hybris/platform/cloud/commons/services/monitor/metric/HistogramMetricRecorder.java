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

import com.codahale.metrics.Histogram;
import de.hybris.platform.cloud.commons.constants.CloudCommonsConstants;
import de.hybris.platform.cloud.commons.services.monitor.*;

/**
 * @inheritDoc
 * Records {@link MonitorHistoryData} as Metric Registry Histogram data
 */
public class HistogramMetricRecorder extends AbstractMetricRecorder
{
	private static final String METRIC_TYPE = "histogram";

	@Override
	protected String getEnabledKey()
	{
		return CloudCommonsConstants.MONITORING_METRIC_HISTOGRAM_ENABLED;
	}

	@Override
	protected void processHistory(final MonitorHistoryData history)
	{
		// Nothing to do
	}

	@Override
	protected void processHistoryEntry(final MonitorHistoryData history, final MonitorHistoryEntryData entry)
	{
		final SystemArea area = history.getSystemArea();
		final Long duration = getDuration(area, entry);
		final Step step = entry.getStep();
		final Status status = entry.getStatus();
		if (duration != null)
		{
			final String key = getStepMetricName(area, step, status.name(), METRIC_TYPE);
			final Histogram histogram = getMetricRegistry().histogram(key);
			histogram.update(duration);
		}
		else
		{
			log.debug("History Entry for Area [{}] Key [{}] Step [{}] Status [{}] had no duration so cannot log", area, history.getKey(), step, status);
		}
	}
}
