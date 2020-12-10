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
import de.hybris.platform.cloud.commons.model.MonitorHistoryEntryActionDataModel;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.core.PK;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MonitorHistoryEntryActionDataConverterTest
{
	private MonitorHistoryEntryActionDataConverter converter = new MonitorHistoryEntryActionDataConverter();

	@Mock
	private ModelService modelService;

	@Mock
	private MonitorHistoryEntryActionDataModel existingModel;
	private PK existingPk = PK.fromLong(1L);

	@Mock
	private MonitorHistoryEntryActionDataModel newModel;

	@Before
	public void setUp()
	{
		converter.setModelService(modelService);

		given(modelService.get(existingPk)).willReturn(existingModel);
		given(modelService.create(MonitorHistoryEntryActionDataModel.class)).willReturn(newModel);
	}

	@Test
	public void convertShouldCreateNewModelWhenHistoryIsNew()
	{
		final Date started = new Date();
		final Date ended = new Date();

		final MonitorHistoryEntryActionData action = MonitorHistoryEntryActionData
				.builder()
				.withCode("code")
				.withStatus(Status.IN_PROGRESS)
				.withStarted(started)
				.withEnded(ended)
				.withDuration(2L)
				.withMessage("message")
				.build();

		final MonitorHistoryEntryActionDataModel model = converter.convert(action);

		assertThat(model).isEqualTo(newModel);
		verify(model).setCode("code");
		verify(model).setStatus(MonitorStatus.IN_PROGRESS);
		verify(model).setStarted(started);
		verify(model).setEnded(ended);
		verify(model).setDuration(2L);
		verify(model).setMessage("message");
		verifyNoMoreInteractions(model);
	}


	@Test
	public void convertShouldUseExistingModelWhenHistoryIsNotNew()
	{
		final MonitorHistoryEntryActionData action = MonitorHistoryEntryActionData.builder()
				.withPk(existingPk)
				.build();

		final MonitorHistoryEntryActionDataModel model = converter.convert(action);

		assertThat(model).isEqualTo(existingModel);
		verifyNoMoreInteractions(model);
	}
}
