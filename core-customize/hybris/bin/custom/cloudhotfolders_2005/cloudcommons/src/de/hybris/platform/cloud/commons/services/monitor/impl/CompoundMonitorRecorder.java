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

import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorRecorder;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

public class CompoundMonitorRecorder implements MonitorRecorder
{
	private List<MonitorRecorder> recorders;

	@Override
	public void record(final MonitorHistoryData history)
	{
		getRecorders().forEach(r -> r.record(history));
	}

	protected List<MonitorRecorder> getRecorders()
	{
		return recorders;
	}

	@Required
	public void setRecorders(final List<MonitorRecorder> recorders)
	{
		this.recorders = recorders;
	}
}
