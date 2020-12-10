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

import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryActionDataModel;
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.database.MonitorHistoryEnumHelper;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Required;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTestMonitorAssertionHelper
{
	private GenericDao<MonitorHistoryDataModel> monitorHistoryDataDao;

	@Required
	public void setMonitorHistoryDataDao(final GenericDao<MonitorHistoryDataModel> monitorHistoryDataDao)
	{
		this.monitorHistoryDataDao = monitorHistoryDataDao;
	}

	public MonitorHistoryData createHistory(final String key, final SystemArea area, final Status status, final MonitorHistoryEntryData... steps)
	{
		return createHistory(key, area, status, Arrays.asList(steps));
	}

	public MonitorHistoryData createHistory(final String key, final SystemArea area, final Status status, final List<MonitorHistoryEntryData> steps)
	{
		final MonitorHistoryData history = new MonitorHistoryData();
		history.setKey(key);
		history.setSystemArea(area);
		history.setStatus(status);
		history.setEntries(steps);
		return history;
	}

	public MonitorHistoryEntryData createStep(final Status status, final Step step, final MonitorHistoryEntryActionData... actions)
	{
		return createStep(status, step, Arrays.asList(actions));
	}

	public MonitorHistoryEntryData createStep(final Status status, final Step step, final List<MonitorHistoryEntryActionData> actions)
	{
		return MonitorHistoryEntryData.builder()
				.withStep(step)
				.withStatus(status)
				.withActions(actions)
				.build();
	}

	public MonitorHistoryEntryActionData createAction(final Status status)
	{
		return MonitorHistoryEntryActionData.builder()
				.withStatus(status)
				.build();
	}

	public void assertHistoryIsPresent(final MonitorHistoryData expectedHistory)
	{
		HashMap<String, Object> params = new HashMap<>();
		params.put(MonitorHistoryDataModel.SYSTEMAREA, MonitorHistoryEnumHelper.getMonitorSystemArea(expectedHistory.getSystemArea()));
		params.put(MonitorHistoryDataModel.KEY, expectedHistory.getKey());

		final List<MonitorHistoryDataModel> histories = monitorHistoryDataDao.find(params);
		assertThat(histories).as("History not found using params: " + params).hasSize(1);

		final SoftAssertions softly = new SoftAssertions();

		final MonitorHistoryDataModel actualHistory = histories.get(0);
		softly.assertThat(actualHistory.getStatus())
				.as("Status of history was not as expected")
				.isEqualTo(MonitorHistoryEnumHelper.getMonitorStatus(expectedHistory.getStatus()));
		assertThatEntriesAreAsExpected(softly, expectedHistory.getEntries(), actualHistory.getEntries());

		softly.assertAll();
	}

	private void assertThatEntriesAreAsExpected(final SoftAssertions softly,
	                                            final List<MonitorHistoryEntryData> expectedEntries,
	                                            final List<MonitorHistoryEntryDataModel> actualEntries)
	{
		final int expectedSize = expectedEntries.size();
		final int actualSize = actualEntries.size();
		if (expectedSize != actualSize)
		{
			softly.assertThat(actualSize)
					.as(String.format("Size of actual steps was not as expected.\r\nExpected [%s]\r\nActual [%s]",
							expectedEntries.stream()
									.map(e -> String.format("Step [%s] Status [%s]", e.getStep(), e.getStatus()))
									.collect(Collectors.joining(", ")),
							actualEntries.stream()
									.map(e -> String.format("Step [%s] Status [%s]", e.getStep(), e.getStatus()))
									.collect(Collectors.joining(", "))
					))
					.isEqualTo(expectedSize);
		}
		else
		{
			for (int i = 0; i < expectedSize; i++)
			{
				final MonitorHistoryEntryData expected = expectedEntries.get(i);
				final MonitorHistoryEntryDataModel actual = actualEntries.get(i);

				softly.assertThat(actual.getStep())
						.as("Step at index [" + i + "] was not as expected")
						.isEqualTo(MonitorHistoryEnumHelper.getMonitorStep(expected.getStep()));
				softly.assertThat(actual.getStatus())
						.as("Status of step at index [" + i + "] was not as expected")
						.isEqualTo(MonitorHistoryEnumHelper.getMonitorStatus(expected.getStatus()));
				assertThatActionsAreAsExpected(softly, expected.getStep(), expected.getActions(), actual.getActions());
			}
		}
	}

	private void assertThatActionsAreAsExpected(final SoftAssertions softly, final Step step,
	                                            final List<MonitorHistoryEntryActionData> expectedActions,
	                                            final List<MonitorHistoryEntryActionDataModel> actualActions)
	{
		final int expectedSize = expectedActions.size();
		final int actualSize = actualActions.size();
		if (expectedSize != actualSize)
		{
			softly.assertThat(actualSize)
					.as(String.format("Size of actual actions for step [%s] was not as expected.\r\nExpected [%s]\r\nActual [%s]",
							step,
							expectedActions.stream()
									.map(e -> String.format("Code [%s] Status [%s]", e.getCode(), e.getStatus()))
									.collect(Collectors.joining(", ")),
							actualActions.stream()
									.map(e -> String.format("Code [%s] Status [%s]", e.getCode(), e.getStatus()))
									.collect(Collectors.joining(", "))
					))
					.isEqualTo(expectedSize);
		}
		else
		{
			for (int i = 0; i < expectedSize; i++)
			{
				final MonitorHistoryEntryActionData expected = expectedActions.get(i);
				final MonitorHistoryEntryActionDataModel actual = actualActions.get(i);

				softly.assertThat(actual.getStatus())
						.as("Status of action at index [" + i + "] in step [" + step + "] was not as expected")
						.isEqualTo(MonitorHistoryEnumHelper.getMonitorStatus(expected.getStatus()));
			}
		}

	}
}
