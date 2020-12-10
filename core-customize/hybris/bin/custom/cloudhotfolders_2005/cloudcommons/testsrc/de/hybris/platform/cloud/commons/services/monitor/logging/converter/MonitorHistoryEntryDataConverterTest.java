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
package de.hybris.platform.cloud.commons.services.monitor.logging.converter;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryActionData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;

import java.util.Collections;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.ISODateTimeFormat;
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
public class MonitorHistoryEntryDataConverterTest
{
	private static final String ACTION_STRING = "actionString";

	private MonitorHistoryEntryDataConverter converter = new MonitorHistoryEntryDataConverter();

	private MonitorHistoryEntryData.Builder entryBuilder = MonitorHistoryEntryData.builder()
			.withStep(Step.PROCESS)
			.withStatus(Status.SUCCESS);

	@Mock
	private Converter<MonitorHistoryEntryActionData, String> entryActionDataConverter;

	private MonitorHistoryEntryActionData action = MonitorHistoryEntryActionData.builder().build();

	@Before
	public void setUp()
	{
		converter.setDateTimeFormatter(ISODateTimeFormat.dateTime());
		converter.setEntryActionDataConverter(entryActionDataConverter);

		given(entryActionDataConverter.convert(action)).willReturn(ACTION_STRING);
	}

	@Test
	public void shouldConvertEntryToExpectedString()
	{
		final String started = "2018-03-15T13:15:10.123Z";
		final DateTime startedDt = DateTime.parse(started);
		final String ended = "2018-03-15T13:15:11.674Z";
		final DateTime endedDt = DateTime.parse(ended);
		final Integer duration = new Period(startedDt, endedDt, PeriodType.millis()).getMillis();
		final int nodeId = 2;
		final String message = "some message goes here";

		final MonitorHistoryEntryData entry = entryBuilder
				.withNodeId(nodeId)
				.withStarted(startedDt.toDate())
				.withEnded(endedDt.toDate())
				.withDuration(duration.longValue())
				.withMessage(message)
				.withActions(Collections.singletonList(action))
				.build();

		assertThat(converter.convert(entry))
				.isEqualTo("Step [PROCESS] Node [2] Status [SUCCESS] " +
						"Started [" + started +
						"] Ended [" + ended +
						"] Duration [" + duration +
						"]ms Message [" + message +
						"] Actions [" + System.lineSeparator() + "\t" + ACTION_STRING + System.lineSeparator() + "]");
	}

	@Test
	public void shouldHandleNulls()
	{
		final MonitorHistoryEntryData entry = entryBuilder.build();

		assertThat(converter.convert(entry))
				.isEqualTo("Step [PROCESS] Node [null] Status [SUCCESS] Started [null] Ended [null] Duration [null]ms Message [null] Actions [none]");
	}
}