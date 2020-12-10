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
package de.hybris.platform.cloud.commons.services.monitor.logging.converter;

import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;

public class MonitorHistoryEntryActionDataConverter extends AbstractMonitorConverter<MonitorHistoryEntryActionData, String>
{
	@Override
	public String convert(final MonitorHistoryEntryActionData e)
	{
		return formatMessage("Code [{}] Status [{}] Started [{}] Ended [{}] Duration [{}]ms Message [{}]",
				e.getCode(), e.getStatus(),
				formatDate(e.getStarted()), formatDate(e.getEnded()), e.getDuration(),
				e.getMessage()
		);
	}
}
