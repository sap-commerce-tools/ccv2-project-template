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
package de.hybris.platform.cloud.commons.services.monitor.impl;

import de.hybris.platform.cloud.commons.services.monitor.MonitorEntryFilter;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;

/**
 * @inheritDoc Filters for {@link MonitorHistoryEntryData} which have not yet been saved
 */
public class UnsavedEntryMonitorEntryFilter implements MonitorEntryFilter
{
	@Override
	public boolean apply(final MonitorHistoryEntryData entry)
	{
		return entry.getPk() == null;
	}
}
