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

import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

public class MonitorHistoryEntryDataConverter extends AbstractMonitorConverter<MonitorHistoryEntryData, String>
{
	private static final String TAB = "\t";

	private Converter<MonitorHistoryEntryActionData, String> entryActionDataConverter;

	@Override
	public String convert(final MonitorHistoryEntryData e)
	{
		final StringBuilder sb = new StringBuilder(
				formatMessage("Step [{}] Node [{}] Status [{}] Started [{}] Ended [{}] Duration [{}]ms Message [{}]",
						e.getStep(), e.getNodeId(), e.getStatus(),
						formatDate(e.getStarted()), formatDate(e.getEnded()), e.getDuration(),
						e.getMessage()));

		if (CollectionUtils.isEmpty(e.getActions()))
		{
			sb.append(" Actions [none]");
		}
		else
		{
			sb.append(" Actions [");
			for (final MonitorHistoryEntryActionData action : e.getActions())
			{
				sb.append(System.lineSeparator()).append(TAB).append(getEntryActionDataConverter().convert(action));
			}
			sb.append(System.lineSeparator()).append("]");
		}

		return sb.toString();
	}

	protected Converter<MonitorHistoryEntryActionData, String> getEntryActionDataConverter()
	{
		return entryActionDataConverter;
	}

	@Required
	public void setEntryActionDataConverter(final Converter<MonitorHistoryEntryActionData, String> entryActionDataConverter)
	{
		this.entryActionDataConverter = entryActionDataConverter;
	}
}
