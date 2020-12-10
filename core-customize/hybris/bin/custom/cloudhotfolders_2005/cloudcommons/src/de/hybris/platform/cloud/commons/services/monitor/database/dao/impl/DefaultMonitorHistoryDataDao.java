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
package de.hybris.platform.cloud.commons.services.monitor.database.dao.impl;

import com.google.common.collect.ImmutableMap;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.services.monitor.database.MonitorHistoryEnumHelper;
import de.hybris.platform.cloud.commons.services.monitor.database.dao.MonitorHistoryDataDao;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;

import java.util.List;


public class DefaultMonitorHistoryDataDao extends DefaultGenericDao<MonitorHistoryDataModel> implements MonitorHistoryDataDao
{
	public DefaultMonitorHistoryDataDao()
	{
		super(MonitorHistoryDataModel._TYPECODE);
	}

	@Override
	public List<MonitorHistoryDataModel> find(final SystemArea area, final String key)
	{
		return find(ImmutableMap.<String, Object>of(MonitorHistoryDataModel.SYSTEMAREA, MonitorHistoryEnumHelper.getMonitorSystemArea(area), MonitorHistoryDataModel.KEY, key));
	}
}
