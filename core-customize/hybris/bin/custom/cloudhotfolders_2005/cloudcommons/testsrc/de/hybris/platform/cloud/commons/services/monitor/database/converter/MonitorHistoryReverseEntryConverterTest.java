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

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MonitorHistoryReverseEntryConverterTest
{
	private static final MonitorStatus STATUS = MonitorStatus.IN_PROGRESS;
	private static final MonitorStep STEP = MonitorStep.PROCESS;
	private static final Status EXPECTED_STATUS = Status.IN_PROGRESS;
	private static final Step EXPECTED_STEP = Step.PROCESS;
	private static final String MESSAGE = "message";
	private PK pk = PK.fromLong(1L);

	private MonitorHistoryReverseEntryConverter converter = new MonitorHistoryReverseEntryConverter();

	@Mock
	private MonitorHistoryEntryDataModel source;
	private Date started = new Date();
	private Date ended = new Date();

	@Mock
	private Converter<MonitorHistoryEntryActionDataModel, MonitorHistoryEntryActionData> actionReverseConverter;

	@Mock
	private MonitorHistoryEntryActionDataModel actionModel;

	@Mock
	private MonitorHistoryEntryActionData actionData;

	@Before
	public void setUp()
	{
		converter.setActionReverseConverter(actionReverseConverter);

		given(source.getPk()).willReturn(pk);
		given(source.getStep()).willReturn(STEP);
		given(source.getStatus()).willReturn(STATUS);
		given(source.getStarted()).willReturn(started);
		given(source.getEnded()).willReturn(ended);
		given(source.getDuration()).willReturn(2L);
		given(source.getNodeId()).willReturn(3);
		given(source.getMessage()).willReturn(MESSAGE);
		given(source.getActions()).willReturn(Collections.singletonList(actionModel));

		given(actionReverseConverter.convert(actionModel)).willReturn(actionData);
	}

	@Test
	public void shouldConvertDataCorrectly()
	{
		final MonitorHistoryEntryData expected = MonitorHistoryEntryData.builder()
				.withPk(pk)
				.withStep(EXPECTED_STEP)
				.withStatus(EXPECTED_STATUS)
				.withStarted(started)
				.withEnded(ended)
				.withDuration(2L)
				.withNodeId(3)
				.withMessage(MESSAGE)
				.withActions(Collections.singletonList(actionData))
				.build();

		assertThat(converter.convert(source)).isEqualToComparingFieldByField(expected);
	}

	@Test
	public void shouldHandleNullActions()
	{
		given(source.getActions()).willReturn(null);

		final MonitorHistoryEntryData expected = MonitorHistoryEntryData.builder()
				.withPk(pk)
				.withStep(EXPECTED_STEP)
				.withStatus(EXPECTED_STATUS)
				.withStarted(started)
				.withEnded(ended)
				.withDuration(2L)
				.withNodeId(3)
				.withMessage(MESSAGE)
				.withActions(Collections.emptyList())
				.build();

		final MonitorHistoryEntryData actual = converter.convert(source);
		assertThat(actual).isEqualToComparingFieldByField(expected);
		assertThat(actual.getActions()).isEmpty();
		actual.addToActions(MonitorHistoryEntryActionData.builder().build());
		assertThat(actual.getActions()).isNotEmpty();
	}

	@Test
	public void shouldBeAbleToAddToActionsAfterwards()
	{
		final MonitorHistoryEntryData withActions = converter.convert(source);
		assertThat(withActions.getActions()).hasSize(1);
		withActions.addToActions(MonitorHistoryEntryActionData.builder().build());
		assertThat(withActions.getActions()).hasSize(2);

		given(source.getActions()).willReturn(null);
		final MonitorHistoryEntryData noActions = converter.convert(source);
		assertThat(noActions.getActions()).isEmpty();
		noActions.addToActions(MonitorHistoryEntryActionData.builder().build());
		assertThat(noActions.getActions()).isNotEmpty();
	}
}