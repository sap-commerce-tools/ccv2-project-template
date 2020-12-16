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
package de.hybris.platform.cloud.commons.services.monitor.logging;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.converter.Converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class LoggingMonitorRecorderTest
{
	private static final String ENABLED = "cloud.commons.monitoring.logging.enabled";

	@Spy
	private LoggingMonitorRecorder recorder = new LoggingMonitorRecorder();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ConfigurationService configurationService;

	private MonitorHistoryData history = new MonitorHistoryData();

	@Mock
	private Converter<MonitorHistoryData, LogData> historyConverter;

	@Before
	public void setUp()
	{
		recorder.setHistoryConverter(historyConverter);
		recorder.setConfigurationService(configurationService);
		recorder.setInfoStatuses(Collections.singletonList(Status.SUCCESS));
		recorder.setErrorStatuses(Collections.singletonList(Status.FAILURE));
		recorder.setWarnStatuses(Collections.singletonList(Status.WARNING));

		given(configurationService.getConfiguration().getBoolean(ENABLED, false)).willReturn(true);

		given(historyConverter.convert(history)).willReturn(LogData.builder().build());
	}

	@Test
	public void shouldNotRecordStatsWhenIsNotEnabled()
	{
		given(configurationService.getConfiguration().getBoolean(ENABLED, false)).willReturn(false);

		recorder.record(history);

		verify(recorder, never()).recordHistory(history);
	}

	@Test
	public void shouldRecordStatsWhenIsEnabled()
	{
		history.setStatus(Status.SUCCESS);

		recorder.record(history);

		verify(historyConverter).convert(history);
	}

	@Test
	public void shouldRecordErrorStatsWhenIsEnabled()
	{
		history.setStatus(Status.FAILURE);

		recorder.record(history);

		verify(historyConverter).convert(history);
	}
}