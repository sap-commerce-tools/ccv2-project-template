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
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.database.MonitorHistoryEnumHelper;
import de.hybris.platform.servicelayer.model.ModelService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

public class MonitorHistoryEntryActionDataConverter implements Converter<MonitorHistoryEntryActionData, MonitorHistoryEntryActionDataModel>
{
	private ModelService modelService;

	@Override
	public MonitorHistoryEntryActionDataModel convert(final MonitorHistoryEntryActionData source)
	{
		return source.getPk() != null
				? getModelService().get(source.getPk())
				: createNew(source);
	}

	protected MonitorHistoryEntryActionDataModel createNew(final MonitorHistoryEntryActionData source)
	{
		final MonitorHistoryEntryActionDataModel target = getModelService().create(MonitorHistoryEntryActionDataModel.class);
		target.setCode(source.getCode());
		target.setStatus(MonitorHistoryEnumHelper.getMonitorStatus(source.getStatus()));
		target.setStarted(source.getStarted());
		target.setEnded(source.getEnded());
		target.setDuration(source.getDuration());
		target.setMessage(source.getMessage());
		return target;
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
}
