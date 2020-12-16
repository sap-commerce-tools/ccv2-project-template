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
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.database.MonitorHistoryEnumHelper;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MonitorHistoryReverseEntryConverter implements Converter<MonitorHistoryEntryDataModel, MonitorHistoryEntryData>
{
	private Converter<MonitorHistoryEntryActionDataModel, MonitorHistoryEntryActionData> actionReverseConverter;

	@Override
	public MonitorHistoryEntryData convert(final MonitorHistoryEntryDataModel source)
	{
		return MonitorHistoryEntryData.builder()
				.withPk(source.getPk())
				.withStep(MonitorHistoryEnumHelper.getStep(source.getStep()))
				.withStatus(MonitorHistoryEnumHelper.getStatus(source.getStatus()))
				.withStarted(source.getStarted())
				.withEnded(source.getEnded())
				.withDuration(source.getDuration())
				.withNodeId(source.getNodeId())
				.withMessage(source.getMessage())
				.withActions(convertActions(source.getActions()))
				.build();
	}

	protected List<MonitorHistoryEntryActionData> convertActions(final List<MonitorHistoryEntryActionDataModel> actions)
	{
		return CollectionUtils.isEmpty(actions)
				? new LinkedList<>()
				: actions.stream().map(getActionReverseConverter()::convert).collect(Collectors.toList());
	}

	protected Converter<MonitorHistoryEntryActionDataModel, MonitorHistoryEntryActionData> getActionReverseConverter()
	{
		return actionReverseConverter;
	}

	@Required
	public void setActionReverseConverter(final Converter<MonitorHistoryEntryActionDataModel, MonitorHistoryEntryActionData> actionReverseConverter)
	{
		this.actionReverseConverter = actionReverseConverter;
	}
}
