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

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MonitorHistoryReverseEntryActionDataConverterTest
{
	private static final MonitorStatus STATUS = MonitorStatus.IN_PROGRESS;
	private static final Status EXPECTED_STATUS = Status.IN_PROGRESS;
	private static final String MESSAGE = "message";
	private static final String CODE = "code";
	private PK pk = PK.fromLong(1L);

	private MonitorHistoryReverseEntryActionDataConverter converter = new MonitorHistoryReverseEntryActionDataConverter();

	@Mock
	private MonitorHistoryEntryActionDataModel source;
	private Date started = new Date();
	private Date ended = new Date();

	@Before
	public void setUp()
	{
		given(source.getPk()).willReturn(pk);
		given(source.getCode()).willReturn(CODE);
		given(source.getStatus()).willReturn(STATUS);
		given(source.getStarted()).willReturn(started);
		given(source.getEnded()).willReturn(ended);
		given(source.getDuration()).willReturn(2L);
		given(source.getMessage()).willReturn(MESSAGE);
	}

	@Test
	public void shouldConvertDataCorrectly()
	{
		final MonitorHistoryEntryActionData expected = MonitorHistoryEntryActionData.builder()
				.withPk(pk)
				.withCode(CODE)
				.withStatus(EXPECTED_STATUS)
				.withStarted(started)
				.withEnded(ended)
				.withDuration(2L)
				.withMessage(MESSAGE)
				.build();

		assertThat(converter.convert(source)).isEqualToComparingFieldByField(expected);
	}
}