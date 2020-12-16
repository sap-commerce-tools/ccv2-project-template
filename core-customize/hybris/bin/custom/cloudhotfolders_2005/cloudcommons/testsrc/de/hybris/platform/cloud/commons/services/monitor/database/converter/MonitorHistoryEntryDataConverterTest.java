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
import de.hybris.platform.cloud.commons.enums.MonitorStep;
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryActionDataModel;
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.core.PK;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Collections;
import java.util.Date;

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
public class MonitorHistoryEntryDataConverterTest
{
	private MonitorHistoryEntryDataConverter converter = new MonitorHistoryEntryDataConverter();

	@Mock
	private ModelService modelService;

	@Mock
	private MonitorHistoryEntryDataModel existingModel;
	private PK existingPk = PK.fromLong(1L);

	@Mock
	private MonitorHistoryEntryDataModel newModel;

	@Mock
	private Converter<MonitorHistoryEntryActionData, MonitorHistoryEntryActionDataModel> actionConverter;

	private MonitorHistoryEntryActionData actionData = MonitorHistoryEntryActionData.builder().build();

	@Mock
	private MonitorHistoryEntryActionDataModel actionModel;

	@Before
	public void setUp()
	{
		converter.setModelService(modelService);
		converter.setActionConverter(actionConverter);

		given(modelService.get(existingPk)).willReturn(existingModel);
		given(modelService.create(MonitorHistoryEntryDataModel.class)).willReturn(newModel);
		given(actionConverter.convert(actionData)).willReturn(actionModel);
	}

	@Test
	public void convertShouldCreateNewModelWhenHistoryIsNew()
	{
		final Date started = new Date();
		final Date ended = new Date();

		final MonitorHistoryEntryData entryData = MonitorHistoryEntryData
				.builder()
				.withStep(Step.PROCESS)
				.withStatus(Status.IN_PROGRESS)
				.withStarted(started)
				.withEnded(ended)
				.withDuration(2L)
				.withNodeId(3)
				.withMessage("message")
				.withActions(Collections.singletonList(actionData))
				.build();

		final MonitorHistoryEntryDataModel model = converter.convert(entryData);

		assertThat(model).isEqualTo(newModel);
		verify(model).setStep(MonitorStep.PROCESS);
		verify(model).setStatus(MonitorStatus.IN_PROGRESS);
		verify(model).setStarted(started);
		verify(model).setEnded(ended);
		verify(model).setDuration(2L);
		verify(model).setNodeId(3);
		verify(model).setMessage("message");
		verify(model).setActions(Collections.singletonList(actionModel));
		verifyNoMoreInteractions(model);
	}

	@Test
	public void convertShouldUseExistingModelWhenHistoryIsNotNew()
	{
		final MonitorHistoryEntryData entryData = MonitorHistoryEntryData.builder()
				.withPk(existingPk)
				.withActions(Collections.singletonList(actionData))
				.build();

		final MonitorHistoryEntryDataModel model = converter.convert(entryData);

		assertThat(model).isEqualTo(existingModel);
		verify(model).setActions(Collections.singletonList(actionModel));
		verifyNoMoreInteractions(model);
	}

	@Test
	public void convertReturnEmptyActionWhenEntryHasNone()
	{
		final MonitorHistoryEntryData entryData = MonitorHistoryEntryData.builder()
				.withPk(existingPk)
				.withActions(null)
				.build();

		final MonitorHistoryEntryDataModel model = converter.convert(entryData);

		assertThat(model).isEqualTo(existingModel);
		verify(model).setActions(Collections.emptyList());
		verifyNoMoreInteractions(model);
	}
}
