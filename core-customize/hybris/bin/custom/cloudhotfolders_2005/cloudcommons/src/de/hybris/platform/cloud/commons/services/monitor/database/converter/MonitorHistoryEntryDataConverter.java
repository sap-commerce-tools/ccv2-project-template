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

import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryActionDataModel;
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.database.MonitorHistoryEnumHelper;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MonitorHistoryEntryDataConverter implements Converter<MonitorHistoryEntryData, MonitorHistoryEntryDataModel>
{
	private ModelService modelService;
	private Converter<MonitorHistoryEntryActionData, MonitorHistoryEntryActionDataModel> actionConverter;

	@Override
	public MonitorHistoryEntryDataModel convert(final MonitorHistoryEntryData source)
	{
		final MonitorHistoryEntryDataModel target = getOrCreate(source);
		target.setActions(convertActions(source.getActions()));
		return target;
	}

	protected MonitorHistoryEntryDataModel getOrCreate(final MonitorHistoryEntryData source)
	{
		return source.getPk() != null
				? getModelService().get(source.getPk())
		        : createNew(source);
	}

	protected MonitorHistoryEntryDataModel createNew(final MonitorHistoryEntryData source)
	{
		final MonitorHistoryEntryDataModel target = getModelService().create(MonitorHistoryEntryDataModel.class);
		target.setStep(MonitorHistoryEnumHelper.getMonitorStep(source.getStep()));
		target.setStatus(MonitorHistoryEnumHelper.getMonitorStatus(source.getStatus()));
		target.setStarted(source.getStarted());
		target.setEnded(source.getEnded());
		target.setDuration(source.getDuration());
		target.setNodeId(source.getNodeId());
		target.setMessage(source.getMessage());
		return target;
	}

	protected List<MonitorHistoryEntryActionDataModel> convertActions(final List<MonitorHistoryEntryActionData> actions)
	{
		return CollectionUtils.isEmpty(actions)
				? Collections.emptyList()
				: actions.stream().map(getActionConverter()::convert).collect(Collectors.toList());
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

	protected Converter<MonitorHistoryEntryActionData, MonitorHistoryEntryActionDataModel> getActionConverter()
	{
		return actionConverter;
	}

	@Required
	public void setActionConverter(final Converter<MonitorHistoryEntryActionData, MonitorHistoryEntryActionDataModel> actionConverter)
	{
		this.actionConverter = actionConverter;
	}
}
