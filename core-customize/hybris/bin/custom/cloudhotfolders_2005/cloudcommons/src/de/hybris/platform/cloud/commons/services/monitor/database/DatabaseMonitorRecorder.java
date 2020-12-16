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
package de.hybris.platform.cloud.commons.services.monitor.database;

import de.hybris.platform.cloud.commons.constants.CloudCommonsConstants;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.impl.AbstractConfigEnabledMonitorRecorder;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.model.ModelService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

/**
 * @inheritDoc
 * Records {@link MonitorHistoryData} into the Hybris type system
 *
 */
public class DatabaseMonitorRecorder extends AbstractConfigEnabledMonitorRecorder
{
	private ModelService modelService;
	private Converter<MonitorHistoryData, MonitorHistoryDataModel> historyConverter;

	@Override
	protected String getEnabledKey()
	{
		return CloudCommonsConstants.MONITORING_DB_ENABLED;
	}

	@Override
	protected void recordHistory(final MonitorHistoryData history)
	{
		final MonitorHistoryDataModel historyModel = getHistoryConverter().convert(history);
		try
		{
			getModelService().save(historyModel);
		}
		catch (final ModelSavingException | IllegalArgumentException e)
		{
			log.error("There was a problem saving the history for Area [{}] Key [{}]", history.getSystemArea(), history.getKey(), e);
		}
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

	protected Converter<MonitorHistoryData, MonitorHistoryDataModel> getHistoryConverter()
	{
		return historyConverter;
	}

	@Required
	public void setHistoryConverter(final Converter<MonitorHistoryData, MonitorHistoryDataModel> historyConverter)
	{
		this.historyConverter = historyConverter;
	}

}
