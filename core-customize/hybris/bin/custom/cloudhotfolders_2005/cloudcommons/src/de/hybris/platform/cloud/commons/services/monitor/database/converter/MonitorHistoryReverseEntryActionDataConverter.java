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
import org.springframework.core.convert.converter.Converter;

public class MonitorHistoryReverseEntryActionDataConverter implements Converter<MonitorHistoryEntryActionDataModel, MonitorHistoryEntryActionData>
{
	@Override
	public MonitorHistoryEntryActionData convert(final MonitorHistoryEntryActionDataModel source)
	{
		return MonitorHistoryEntryActionData.builder()
				.withPk(source.getPk())
				.withCode(source.getCode())
				.withStatus(MonitorHistoryEnumHelper.getStatus(source.getStatus()))
				.withStarted(source.getStarted())
				.withEnded(source.getEnded())
				.withDuration(source.getDuration())
				.withMessage(source.getMessage())
				.build();
	}
}
