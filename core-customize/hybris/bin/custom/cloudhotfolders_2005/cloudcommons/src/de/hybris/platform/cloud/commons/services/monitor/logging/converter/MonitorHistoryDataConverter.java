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
package de.hybris.platform.cloud.commons.services.monitor.logging.converter;

import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.logging.LogData;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

public class MonitorHistoryDataConverter extends AbstractMonitorConverter<MonitorHistoryData, LogData>
{
	private Converter<MonitorHistoryEntryData, String> entryConverter;

	@Override
	public LogData convert(final MonitorHistoryData history)
	{
		final StringBuilder sb = new StringBuilder();

		sb.append(formatMessage("{}:{} Status [{}]", history.getSystemArea(), history.getKey(), history.getStatus()));

		for (final MonitorHistoryEntryData e : history.getEntries())
		{
			sb.append(System.lineSeparator());
			sb.append(getEntryConverter().convert(e));
		}

		return LogData.builder()
				.withProcessedMessage(sb.toString())
				.withExceptionMessage(history.getExceptionMessage())
				.withException(history.getException())
				.build();
	}

	protected Converter<MonitorHistoryEntryData, String> getEntryConverter()
	{
		return entryConverter;
	}

	@Required
	public void setEntryConverter(final Converter<MonitorHistoryEntryData, String> entryConverter)
	{
		this.entryConverter = entryConverter;
	}
}
