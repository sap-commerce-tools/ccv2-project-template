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

import de.hybris.platform.cloud.commons.enums.MonitorStatus;
import de.hybris.platform.cloud.commons.enums.MonitorStep;
import de.hybris.platform.cloud.commons.enums.MonitorSystemArea;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.utils.EnumHelper;

public final class MonitorHistoryEnumHelper
{
	private MonitorHistoryEnumHelper() {}

	public static MonitorSystemArea getMonitorSystemArea(final SystemArea area) {
		return EnumHelper.getEnum(MonitorSystemArea.class, area);
	}

	public static MonitorStatus getMonitorStatus(final Status status) {
		return EnumHelper.getEnum(MonitorStatus.class, status);
	}

	public static MonitorStep getMonitorStep(final Step step) {
		return EnumHelper.getEnum(MonitorStep.class, step);
	}

	public static SystemArea getSystemArea(final MonitorSystemArea area) {
		return EnumHelper.getEnum(SystemArea.class, area);
	}

	public static Status getStatus(final MonitorStatus status) {
		return EnumHelper.getEnum(Status.class, status);
	}

	public static Step getStep(final MonitorStep step) {
		return EnumHelper.getEnum(Step.class, step);
	}
}
