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
package de.hybris.platform.cloud.commons.services.monitor.metric;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.constants.CloudCommonsConstants;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.services.monitor.impl.UnsavedEntryMonitorEntryFilter;
import de.hybris.platform.core.PK;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class HistogramMetricRecorderTest
{
	private static final String ENABLED = "cloud.commons.monitoring.metric.histogram.enabled";
	private static final String KEY = "key";
	private static final SystemArea AREA = SystemArea.INTEGRATION;
	private static final String TENANT_ID = "master";

	private static final Step STEP = Step.PROCESS;
	private static final Status STEP_STATUS = Status.FAILURE;

	private static final String STEP_METRIC = String.format("tenant=%s,extension=%s,area=%s,step=%s,name=%s",
			TENANT_ID, CloudCommonsConstants.EXTENSIONNAME, AREA.name(), STEP.name(), STEP_STATUS.name() + ".histogram");

	@Spy
	private HistogramMetricRecorder recorder = new HistogramMetricRecorder();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ConfigurationService configurationService;

	@Mock
	private MetricRegistry metricRegistry;

	private MonitorHistoryData history = new MonitorHistoryData();

	@Mock
	private Histogram stepMetric;

	@Before
	public void setUp()
	{
		willReturn(TENANT_ID).given(recorder).getTenantId();

		recorder.setEntryFilter(new UnsavedEntryMonitorEntryFilter());
		recorder.setConfigurationService(configurationService);
		recorder.setMetricRegistry(metricRegistry);

		history.setSystemArea(AREA);
		history.setKey(KEY);
		history.setEntries(Collections.emptyList());

		given(metricRegistry.histogram(STEP_METRIC)).willReturn(stepMetric);

		given(configurationService.getConfiguration().getBoolean(ENABLED, false)).willReturn(true);
	}

	@Test
	public void shouldNotRecordStatsWhenIsNotEnabled()
	{
		given(configurationService.getConfiguration().getBoolean(ENABLED, false)).willReturn(false);

		recorder.record(history);

		verify(recorder, never()).processHistory(history);
	}

	@Test
	public void shouldDoNothingForStatusOfHistory()
	{
		recorder.record(history);

		verifyNoMoreInteractions(metricRegistry);
	}

	@Test
	public void shouldNotRecordMetricOfPreviouslySavedEntries() {
		final MonitorHistoryEntryData savedEntry = MonitorHistoryEntryData.builder()
				.withPk(PK.fromLong(1L))
				.build();

		history.setEntries(Collections.singletonList(savedEntry));

		recorder.record(history);

		verify(recorder, never()).processHistoryEntry(history, savedEntry);
	}

	@Test
	public void shouldRecordDurationOfUnsavedSavedEntries() {
		final MonitorHistoryEntryData newEntry = MonitorHistoryEntryData.builder()
				.withStep(STEP)
				.withStatus(STEP_STATUS)
				.withDuration(5L)
				.build();

		history.setEntries(Collections.singletonList(newEntry));

		recorder.record(history);

		verify(stepMetric).update(5L);
	}

	@Test
	public void shouldRecordCalculatedDurationOfUnsavedSavedEntries() {
		final DateTime start = DateTime.now();
		final Long duration = 15L;
		final DateTime end = start.plusMillis(duration.intValue());

		final MonitorHistoryEntryData newEntry = MonitorHistoryEntryData.builder()
				.withStep(STEP)
				.withStatus(STEP_STATUS)
				.withStarted(start.toDate())
				.withEnded(end.toDate())
				.build();

		history.setEntries(Collections.singletonList(newEntry));

		recorder.record(history);

		verify(stepMetric).update(duration);
	}

	@Test
	public void shouldNotRecordUnsavedSavedEntryWhereHasNoDuration() {

		final MonitorHistoryEntryData newEntry = MonitorHistoryEntryData.builder()
				.withStep(STEP)
				.withStatus(STEP_STATUS)
				.build();

		history.setEntries(Collections.singletonList(newEntry));

		recorder.record(history);

		verify(stepMetric, never()).update(anyLong());
	}
}
