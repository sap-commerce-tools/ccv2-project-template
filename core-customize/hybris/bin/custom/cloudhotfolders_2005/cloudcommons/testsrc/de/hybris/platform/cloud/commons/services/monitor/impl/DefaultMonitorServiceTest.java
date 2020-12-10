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

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryFactory;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryRestorer;
import de.hybris.platform.cloud.commons.services.monitor.MonitorRecorder;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.services.monitor.impl.DefaultMonitorService.DefaultMonitorHistory;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultMonitorServiceTest
{
	private static final String KEY = "key";
	private static final int NODE_ID = 11;
	private static final SystemArea AREA = SystemArea.INTEGRATION;

	@Spy
	private DefaultMonitorService service = new DefaultMonitorService();

	@Mock
	private MonitorRecorder monitorRecorder;

	@Mock
	private MonitorHistoryRestorer resumeMonitorRestorer;

	@Spy
	private MonitorHistoryFactory historyFactory = new MonitorHistoryFactory();

	@Before
	public void setUp()
	{
		service.setMonitorRecorder(monitorRecorder);
		service.setResumeMonitorRestorer(resumeMonitorRestorer);
		service.setHistoryFactory(historyFactory);

		willReturn(NODE_ID).given(service).getClusterId();
	}

	@Test
	public void service_beginShouldReturnNewHistoryAndSetItAsThreadLocal()
	{
		final MonitorHistoryData expected = new MonitorHistoryData();
		expected.setKey(KEY);
		expected.setSystemArea(AREA);
		expected.setStatus(Status.IN_PROGRESS);
		expected.setEntries(new LinkedList<>());

		final DefaultMonitorHistory actual = (DefaultMonitorHistory) service.begin(AREA, KEY);

		assertThat(actual.getHistory()).isEqualToComparingFieldByField(expected);
		assertThat(service.current()).isEqualTo(Optional.of(actual));
	}

	@Test
	public void service_currentShouldReturnEmptyWhenNoSet()
	{
		assertThat(service.current()).isEmpty();
	}

	@Test
	public void service_resumeShouldReturnExistingHistoryAndSetItAsThreadLocal()
	{
		final MonitorHistoryData expected = historyFactory.create(AREA, KEY);
		given(resumeMonitorRestorer.restore(AREA, KEY)).willReturn(expected);

		final DefaultMonitorHistory actual = (DefaultMonitorHistory) service.resume(AREA, KEY);

		final MonitorHistoryData history = actual.getHistory();
		assertThat(history).isEqualTo(expected);
		assertThat(service.current()).isEqualTo(Optional.of(actual));
	}

	@Test
	public void history_stepSucceededShouldAddEntryWithDataIntoEntries()
	{
		final MonitorHistoryData expected = historyFactory.create(AREA, KEY);
		given(resumeMonitorRestorer.restore(AREA, KEY)).willReturn(expected);

		final DefaultMonitorHistory originalHistory = (DefaultMonitorHistory) service.resume(AREA, KEY);

		final DateTime now = DateTime.now();
		final Integer duration = 5;
		final Date start = now.toDate();
		final Date end = now.plusMillis(duration).toDate();

		final DefaultMonitorHistory newHistory = originalHistory.stepSucceeded(Step.PROCESS, start, end, "Arg [{}]", "val");

		assertThat(newHistory).isSameAs(originalHistory);

		final MonitorHistoryEntryData expectedEntry = MonitorHistoryEntryData.builder()
				.withStep(Step.PROCESS)
				.withStatus(Status.SUCCESS)
				.withStarted(start)
				.withEnded(end)
				.withDuration(duration.longValue())
				.withMessage("Arg [val]")
				.withNodeId(NODE_ID)
				.withActions(Collections.emptyList())
				.build();

		final List<MonitorHistoryEntryData> actualEntries = newHistory.getHistory().getEntries();
		assertThat(actualEntries).hasSize(1);
		assertThat(actualEntries.get(0)).isEqualToComparingFieldByField(expectedEntry);
	}

	@Test
	public void history_stepFailedShouldAddEntryWithDataIntoEntries()
	{
		final MonitorHistoryData expected = historyFactory.create(AREA, KEY);
		given(resumeMonitorRestorer.restore(AREA, KEY)).willReturn(expected);

		final DefaultMonitorHistory originalHistory = (DefaultMonitorHistory) service.resume(AREA, KEY);

		final DateTime now = DateTime.now();
		final Integer duration = 5;
		final Date start = now.toDate();
		final Date end = now.plusMillis(duration).toDate();

		final IllegalArgumentException exception = new IllegalArgumentException("some error message");
		final DefaultMonitorHistory newHistory = originalHistory.stepFailed(Step.PROCESS, start, end, exception, "Argument [{}]", "value");

		assertThat(newHistory).isSameAs(originalHistory);

		assertThat(newHistory.getHistory().getException()).isEqualTo(exception);
		assertThat(newHistory.getHistory().getExceptionMessage()).isEqualTo("some error message");

		final MonitorHistoryEntryData expectedEntry = MonitorHistoryEntryData.builder()
				.withStep(Step.PROCESS)
				.withStatus(Status.FAILURE)
				.withStarted(start)
				.withEnded(end)
				.withDuration(duration.longValue())
				.withMessage("Argument [value]")
				.withNodeId(NODE_ID)
				.withActions(Collections.emptyList())
				.build();

		final List<MonitorHistoryEntryData> actualEntries = newHistory.getHistory().getEntries();
		assertThat(actualEntries).hasSize(1);
		assertThat(actualEntries.get(0)).isEqualToComparingFieldByField(expectedEntry);
	}

	@Test
	public void history_checkpointShouldRecordHistoryAndRemoveCurrentValue()
	{
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);
		final MonitorHistoryData historyData = monitorHistory.getHistory();

		monitorHistory.checkpoint();

		verify(monitorRecorder).record(historyData);
		assertThat(service.current()).isEmpty();
	}

	@Test
	public void history_endShouldSetStatusAndRecordHistoryAndRemoveCurrentValue()
	{
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);
		final MonitorHistoryData historyData = monitorHistory.getHistory();

		monitorHistory.stepSucceeded(Step.PROCESS, null, null, null)
				.end(Status.SUCCESS);

		assertThat(historyData.getStatus()).isEqualTo(Status.SUCCESS);
		verify(monitorRecorder).record(historyData);
		assertThat(service.current()).isEmpty();
	}

	@Test
	public void history_endShouldRetainFailedStatusWhenAnyEntryIsNotSuccessful()
	{
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);
		final MonitorHistoryData historyData = monitorHistory.getHistory();

		monitorHistory.stepFailed(Step.PROCESS, null, null, null, null)
				.end(Status.FAILURE);

		assertThat(historyData.getStatus()).isEqualTo(Status.FAILURE);
		verify(monitorRecorder).record(historyData);
		assertThat(service.current()).isEmpty();
	}

	@Test
	public void history_endShouldAdjustSuccessStatusToWarningWhenAnyEntryIsNotSuccessful()
	{
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);
		final MonitorHistoryData historyData = monitorHistory.getHistory();

		monitorHistory.stepFailed(Step.PROCESS, null, null, null, null)
				.end(Status.SUCCESS);

		assertThat(historyData.getStatus()).isEqualTo(Status.WARNING);
		verify(monitorRecorder).record(historyData);
		assertThat(service.current()).isEmpty();
	}

	@Test
	public void history_addActionShouldNotModifyHistoryData()
	{
		final MonitorHistoryData historyData = mock(MonitorHistoryData.class);
		given(historyFactory.create(AREA, KEY)).willReturn(historyData);
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);

		monitorHistory.addAction("code", Status.IN_PROGRESS, new Date(), new Date(), "message");

		verifyNoMoreInteractions(historyData);
	}

	@Test
	public void history_actionsAddedShouldBeIncludedInSuccessfulStepAdded()
	{
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);

		final DateTime now = DateTime.now();
		final Integer duration = 5;
		final Date actionStart = now.toDate();
		final Date actionEnd = now.plusMillis(duration).toDate();
		final Date stepStart = now.minusSeconds(duration).toDate();
		final Date stepEnd = now.minusSeconds(duration).plusMillis(duration).toDate();

		monitorHistory.addAction("code", Status.IN_PROGRESS, actionStart, actionEnd, "message")
				.stepSucceeded(Step.PROCESS, stepStart, stepEnd, "Arg [{}]", "val");

		final MonitorHistoryEntryActionData expectedAction = MonitorHistoryEntryActionData.builder()
				.withCode("code")
				.withStatus(Status.IN_PROGRESS)
				.withStarted(actionStart)
				.withEnded(actionEnd)
				.withDuration(duration.longValue())
				.withMessage("message")
				.build();

		final MonitorHistoryEntryData expectedEntry = MonitorHistoryEntryData.builder()
				.withStep(Step.PROCESS)
				.withStatus(Status.SUCCESS)
				.withStarted(stepStart)
				.withEnded(stepEnd)
				.withDuration(duration.longValue())
				.withMessage("Arg [val]")
				.withNodeId(NODE_ID)
				.withActions(Collections.singletonList(expectedAction))
				.build();

		final List<MonitorHistoryEntryData> actualEntries = monitorHistory.getHistory().getEntries();
		assertThat(actualEntries).hasSize(1);
		assertThat(actualEntries.get(0)).isEqualToIgnoringGivenFields(expectedEntry, "actions");
		assertThat(actualEntries.get(0).getActions()).hasSize(1);
		assertThat(actualEntries.get(0).getActions().get(0)).isEqualToComparingFieldByField(expectedAction);
	}

	@Test
	public void history_actionsAddedShouldBeIncludedInFailedStepAdded()
	{
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);

		final DateTime now = DateTime.now();
		final Integer duration = 5;
		final Date actionStart = now.toDate();
		final Date actionEnd = now.plusMillis(duration).toDate();
		final Date stepStart = now.minusSeconds(duration).toDate();
		final Date stepEnd = now.minusSeconds(duration).plusMillis(duration).toDate();

		final RuntimeException ex = new RuntimeException("random message");

		monitorHistory.addAction("code", Status.FAILURE, actionStart, actionEnd, "message")
				.stepFailed(Step.PROCESS, stepStart, stepEnd, ex,"Arg [{}]", "val");

		final MonitorHistoryEntryActionData expectedAction = MonitorHistoryEntryActionData.builder()
				.withCode("code")
				.withStatus(Status.FAILURE)
				.withStarted(actionStart)
				.withEnded(actionEnd)
				.withDuration(duration.longValue())
				.withMessage("message")
				.build();

		final MonitorHistoryEntryData expectedEntry = MonitorHistoryEntryData.builder()
				.withStep(Step.PROCESS)
				.withStatus(Status.FAILURE)
				.withStarted(stepStart)
				.withEnded(stepEnd)
				.withDuration(duration.longValue())
				.withMessage("Arg [val]")
				.withNodeId(NODE_ID)
				.withActions(Collections.singletonList(expectedAction))
				.build();

		final MonitorHistoryData history = monitorHistory.getHistory();
		assertThat(history.getException()).isEqualTo(ex);
		assertThat(history.getExceptionMessage()).isEqualTo(ex.getMessage());

		final List<MonitorHistoryEntryData> actualEntries = history.getEntries();
		assertThat(actualEntries).hasSize(1);
		assertThat(actualEntries.get(0)).isEqualToIgnoringGivenFields(expectedEntry, "actions");
		assertThat(actualEntries.get(0).getActions()).hasSize(1);
		assertThat(actualEntries.get(0).getActions().get(0)).isEqualToComparingFieldByField(expectedAction);
	}

	@Test
	public void history_failedActionsAddedShouldBeSetSuccessfulStepAsWarningInstead()
	{
		final DefaultMonitorHistory monitorHistory = (DefaultMonitorHistory) service.begin(AREA, KEY);

		final DateTime now = DateTime.now();
		final Integer duration = 5;
		final Date actionStart = now.toDate();
		final Date actionEnd = now.plusMillis(duration).toDate();
		final Date stepStart = now.minusSeconds(duration).toDate();
		final Date stepEnd = now.minusSeconds(duration).plusMillis(duration).toDate();

		monitorHistory.addAction("code", Status.FAILURE, actionStart, actionEnd, "message")
				.stepSucceeded(Step.PROCESS, stepStart, stepEnd, "Arg [{}]", "val");

		final MonitorHistoryEntryActionData expectedAction = MonitorHistoryEntryActionData.builder()
				.withCode("code")
				.withStatus(Status.FAILURE)
				.withStarted(actionStart)
				.withEnded(actionEnd)
				.withDuration(duration.longValue())
				.withMessage("message")
				.build();

		final MonitorHistoryEntryData expectedEntry = MonitorHistoryEntryData.builder()
				.withStep(Step.PROCESS)
				.withStatus(Status.WARNING)
				.withStarted(stepStart)
				.withEnded(stepEnd)
				.withDuration(duration.longValue())
				.withMessage("Arg [val]")
				.withNodeId(NODE_ID)
				.withActions(Collections.singletonList(expectedAction))
				.build();

		final List<MonitorHistoryEntryData> actualEntries = monitorHistory.getHistory().getEntries();
		assertThat(actualEntries).hasSize(1);
		assertThat(actualEntries.get(0)).isEqualToIgnoringGivenFields(expectedEntry, "actions");
		assertThat(actualEntries.get(0).getActions()).hasSize(1);
		assertThat(actualEntries.get(0).getActions().get(0)).isEqualToComparingFieldByField(expectedAction);
	}
}