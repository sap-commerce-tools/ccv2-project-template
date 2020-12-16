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
package de.hybris.platform.cloud.commons.services.monitor.database.converter;

import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryFactory;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.services.monitor.database.MonitorHistoryEnumHelper;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

public class MonitorHistoryReverseConverter implements Converter<MonitorHistoryDataModel, MonitorHistoryData>
{
	private Converter<MonitorHistoryEntryDataModel, MonitorHistoryEntryData> entryReverseConverter;
	private MonitorHistoryFactory historyFactory;

	@Override
	public MonitorHistoryData convert(final MonitorHistoryDataModel source)
	{
		final SystemArea area = MonitorHistoryEnumHelper.getSystemArea(source.getSystemArea());
		final MonitorHistoryData target = getHistoryFactory().create(area, source.getKey());
		target.setPk(source.getPk());
		target.setStatus(MonitorHistoryEnumHelper.getStatus(source.getStatus()));
		target.setStatusWhenResumed(MonitorHistoryEnumHelper.getStatus(source.getStatus()));
		target.setExceptionMessage(source.getExceptionMessage());
		if (!(source.getEntries() == null || source.getEntries().isEmpty()))
		{
			final List<MonitorHistoryEntryData> entries = target.getEntries();
			source.getEntries().forEach(e -> entries.add(getEntryReverseConverter().convert(e)));
		}
		return target;
	}

	protected Converter<MonitorHistoryEntryDataModel, MonitorHistoryEntryData> getEntryReverseConverter()
	{
		return entryReverseConverter;
	}

	@Required
	public void setEntryReverseConverter(final Converter<MonitorHistoryEntryDataModel, MonitorHistoryEntryData> entryReverseConverter)
	{
		this.entryReverseConverter = entryReverseConverter;
	}

	protected MonitorHistoryFactory getHistoryFactory()
	{
		return historyFactory;
	}

	@Required
	public void setHistoryFactory(final MonitorHistoryFactory historyFactory)
	{
		this.historyFactory = historyFactory;
	}
}
