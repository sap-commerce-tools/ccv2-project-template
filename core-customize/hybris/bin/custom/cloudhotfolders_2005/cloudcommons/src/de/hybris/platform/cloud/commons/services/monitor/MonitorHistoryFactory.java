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
package de.hybris.platform.cloud.commons.services.monitor;

import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * Factory to create a new {@link MonitorHistoryData}
 */
public class MonitorHistoryFactory
{
	/**
	 * Create a new {@link MonitorHistoryData}
	 * @param area being recorded
	 * @param key unique key to identify the history being recorded
	 * @return {@link MonitorHistoryData} with area and key set, status {@link Status#IN_PROGRESS} and entries as new List
	 */
	public MonitorHistoryData create(final SystemArea area, final String key)
	{
		Assert.notNull(area, "'SystemArea' cannot be null");
		Assert.isTrue(StringUtils.isNotBlank(key), "'key' cannot be null, empty for blank");
		final MonitorHistoryData history = new MonitorHistoryData();
		history.setKey(key);
		history.setSystemArea(area);
		history.setStatus(Status.IN_PROGRESS);
		history.setEntries(new LinkedList<>());
		return history;
	}
}
