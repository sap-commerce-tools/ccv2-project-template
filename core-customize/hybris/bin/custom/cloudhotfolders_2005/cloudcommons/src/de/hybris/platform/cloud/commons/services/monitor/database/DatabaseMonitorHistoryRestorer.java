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

import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryFactory;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryRestorer;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.services.monitor.database.dao.MonitorHistoryDataDao;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class DatabaseMonitorHistoryRestorer implements MonitorHistoryRestorer
{
	private static final Logger LOG = getLogger(DatabaseMonitorHistoryRestorer.class);

	private MonitorHistoryDataDao monitoryHistoryDao;
	private Converter<MonitorHistoryDataModel, MonitorHistoryData> historyReverseConverter;
	private MonitorHistoryFactory historyFactory;

	@Override
	public MonitorHistoryData restore(final SystemArea area, final String key)
	{
		final List<MonitorHistoryDataModel> models = getSavedModels(area, key);
		if (CollectionUtils.isNotEmpty(models))
		{
			if (models.size() > 1)
			{
				LOG.error("There were more than 1 existing records for Area [{}] Key [{}], will restore the first", area, key);
			}
			else
			{
				LOG.debug("There was an existing record for Area [{}] Key [{}] will restore information", area, key);
			}
			return getHistoryReverseConverter().convert(models.get(0));
		}
		else
		{
			LOG.debug("There were no existing records for Area [{}] Key [{}]", area, key);
			return getHistoryFactory().create(area, key);
		}
	}

	protected List<MonitorHistoryDataModel> getSavedModels(final SystemArea area, final String key)
	{
		try
		{
			return getMonitoryHistoryDao().find(area, key);
		}
		catch (final IllegalArgumentException e)
		{
			LOG.error("There was a problem retrieving the saved models", e);
			return Collections.emptyList();
		}
	}

	protected MonitorHistoryDataDao getMonitoryHistoryDao()
	{
		return monitoryHistoryDao;
	}

	@Required
	public void setMonitoryHistoryDao(final MonitorHistoryDataDao monitoryHistoryDao)
	{
		this.monitoryHistoryDao = monitoryHistoryDao;
	}

	protected Converter<MonitorHistoryDataModel, MonitorHistoryData> getHistoryReverseConverter()
	{
		return historyReverseConverter;
	}

	@Required
	public void setHistoryReverseConverter(final Converter<MonitorHistoryDataModel, MonitorHistoryData> historyReverseConverter)
	{
		this.historyReverseConverter = historyReverseConverter;
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
