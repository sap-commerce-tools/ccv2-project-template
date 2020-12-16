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

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.enums.MonitorStatus;
import de.hybris.platform.cloud.commons.enums.MonitorSystemArea;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.core.PK;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MonitorHistoryDataConverterTest
{
	private static final SystemArea AREA = SystemArea.INTEGRATION;
	private static final String KEY = "key";
	private static final String EXCEPTION_MESSAGE = "exceptionMessage";
	private static final Status STATUS = Status.IN_PROGRESS;
	private static final MonitorSystemArea EXPECTED_AREA = MonitorSystemArea.INTEGRATION;
	private static final MonitorStatus EXPECTED_STATUS = MonitorStatus.IN_PROGRESS;

	private MonitorHistoryDataConverter converter = new MonitorHistoryDataConverter();

	@Mock
	private ModelService modelService;

	@Mock
	private Converter<MonitorHistoryEntryData, MonitorHistoryEntryDataModel> entryConverter;

	private MonitorHistoryData historyData = new MonitorHistoryData();

	@Mock
	private MonitorHistoryDataModel existingModel;
	private PK existingPk = PK.fromLong(1L);

	@Mock
	private MonitorHistoryDataModel newModel;

	private MonitorHistoryEntryData entryData = MonitorHistoryEntryData.builder().build();

	@Mock
	private MonitorHistoryEntryDataModel entryModel;

	@Before
	public void setUp()
	{
		converter.setModelService(modelService);
		converter.setEntryConverter(entryConverter);

		historyData.setSystemArea(AREA);
		historyData.setKey(KEY);
		historyData.setStatus(STATUS);
		historyData.setExceptionMessage(EXCEPTION_MESSAGE);
		historyData.setEntries(Collections.singletonList(entryData));

		given(modelService.get(existingPk)).willReturn(existingModel);
		given(modelService.create(MonitorHistoryDataModel.class)).willReturn(newModel);

		given(entryConverter.convert(entryData)).willReturn(entryModel);
	}

	@Test
	public void convertShouldCreateNewModelWhenHistoryIsNew()
	{
		historyData.setPk(null);

		final MonitorHistoryDataModel model = converter.convert(historyData);

		assertThat(model).isEqualTo(newModel);
		verify(model).setSystemArea(EXPECTED_AREA);
		verify(model).setKey(KEY);
		verify(model).setStatus(EXPECTED_STATUS);
		verify(model).setExceptionMessage(EXCEPTION_MESSAGE);
		verify(model).setEntries(Collections.singletonList(entryModel));
	}

	@Test
	public void convertShouldUseExistingModelWhenHistoryIsNotNew()
	{
		historyData.setPk(existingPk);

		final MonitorHistoryDataModel model = converter.convert(historyData);

		assertThat(model).isEqualTo(existingModel);
		verify(model).setStatus(EXPECTED_STATUS);
		verify(model).setExceptionMessage(EXCEPTION_MESSAGE);
		verify(model).setEntries(Collections.singletonList(entryModel));
		verifyNoMoreInteractions(model);
	}

	@Test
	public void convertReturnEmptyEntriesWhenHistoryHasNone()
	{
		historyData.setPk(null);
		historyData.setEntries(null);

		final MonitorHistoryDataModel model = converter.convert(historyData);

		assertThat(model).isEqualTo(newModel);
		verify(model).setSystemArea(EXPECTED_AREA);
		verify(model).setKey(KEY);
		verify(model).setStatus(EXPECTED_STATUS);
		verify(model).setExceptionMessage(EXCEPTION_MESSAGE);
		verify(model).setEntries(Collections.emptyList());
		verifyNoMoreInteractions(model);
	}
}
