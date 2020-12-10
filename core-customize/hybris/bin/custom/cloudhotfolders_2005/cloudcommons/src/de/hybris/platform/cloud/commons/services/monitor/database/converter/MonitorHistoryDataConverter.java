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
import de.hybris.platform.cloud.commons.services.monitor.database.MonitorHistoryEnumHelper;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MonitorHistoryDataConverter implements Converter<MonitorHistoryData, MonitorHistoryDataModel>
{
	private ModelService modelService;
	private Converter<MonitorHistoryEntryData, MonitorHistoryEntryDataModel> entryConverter;

	@Override
	public MonitorHistoryDataModel convert(final MonitorHistoryData source)
	{
		final MonitorHistoryDataModel target = getOrCreate(source);
		target.setStatus(MonitorHistoryEnumHelper.getMonitorStatus(source.getStatus()));
		target.setExceptionMessage(source.getExceptionMessage());
		target.setEntries(convertEntries(source.getEntries()));
		return target;
	}

	protected MonitorHistoryDataModel getOrCreate(final MonitorHistoryData source)
	{
		if (source.getPk() != null)
		{
			return getModelService().get(source.getPk());
		}
		else
		{
			final MonitorHistoryDataModel target = getModelService().create(MonitorHistoryDataModel.class);
			target.setSystemArea(MonitorHistoryEnumHelper.getMonitorSystemArea(source.getSystemArea()));
			target.setKey(source.getKey());
			return target;
		}
	}

	protected List<MonitorHistoryEntryDataModel> convertEntries(final List<MonitorHistoryEntryData> entries)
	{
		return CollectionUtils.isEmpty(entries)
				? Collections.emptyList()
				: entries.stream().map(getEntryConverter()::convert).collect(Collectors.toList());
	}

	protected ModelService getModelService()
	{
		return modelService;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	protected Converter<MonitorHistoryEntryData, MonitorHistoryEntryDataModel> getEntryConverter()
	{
		return entryConverter;
	}

	@Required
	public void setEntryConverter(final Converter<MonitorHistoryEntryData, MonitorHistoryEntryDataModel> entryConverter)
	{
		this.entryConverter = entryConverter;
	}

}
