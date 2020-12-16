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

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.model.ModelService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.converter.Converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DatabaseMonitorRecorderTest
{
	private static final String ENABLED = "cloud.commons.monitoring.database.enabled";

	private DatabaseMonitorRecorder recorder = new DatabaseMonitorRecorder();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ConfigurationService configurationService;

	@Mock
	private ModelService modelService;

	@Mock
	private Converter<MonitorHistoryData, MonitorHistoryDataModel> historyConverter;

	private MonitorHistoryData historyData = new MonitorHistoryData();

	@Mock
	private MonitorHistoryDataModel historyModel;

	@Before
	public void setUp()
	{
		recorder.setModelService(modelService);
		recorder.setHistoryConverter(historyConverter);
		recorder.setConfigurationService(configurationService);

		given(configurationService.getConfiguration().getBoolean(ENABLED, false)).willReturn(true);

		given(historyConverter.convert(historyData)).willReturn(historyModel);
	}

	@Test
	public void shouldNotRecordWhenIsNotEnabled()
	{
		given(configurationService.getConfiguration().getBoolean(ENABLED, false)).willReturn(false);

		recorder.record(historyData);

		verify(modelService, never()).save(historyModel);
	}

	@Test
	public void recordShouldSaveConvertedHistory()
	{
		recorder.record(historyData);

		verify(modelService).save(historyModel);
	}

	@Test
	public void recordShouldCatchAndLogModelSavingExceptions()
	{
		willThrow(new ModelSavingException("oh no")).given(modelService).save(historyModel);

		recorder.record(historyData);
	}
}