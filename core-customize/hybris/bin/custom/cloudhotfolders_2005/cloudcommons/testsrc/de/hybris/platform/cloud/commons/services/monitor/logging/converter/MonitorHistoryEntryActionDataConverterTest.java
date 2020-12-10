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
import de.hybris.platform.cloud.commons.services.monitor.Status;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class MonitorHistoryEntryActionDataConverterTest
{
	private static final String CODE = "uniqueCode";

	private MonitorHistoryEntryActionDataConverter converter = new MonitorHistoryEntryActionDataConverter();

	private MonitorHistoryEntryActionData.Builder entryBuilder = MonitorHistoryEntryActionData.builder()
			.withCode(CODE)
			.withStatus(Status.SUCCESS);

	@Before
	public void setUp()
	{
		converter.setDateTimeFormatter(ISODateTimeFormat.dateTime());
	}

	@Test
	public void shouldConvertEntryToExpectedString()
	{
		final String started = "2018-03-15T13:15:10.123Z";
		final DateTime startedDt = DateTime.parse(started);
		final String ended = "2018-03-15T13:15:11.674Z";
		final DateTime endedDt = DateTime.parse(ended);
		final Integer duration = new Period(startedDt, endedDt, PeriodType.millis()).getMillis();
		final String message = "some message goes here";

		final MonitorHistoryEntryActionData entry = entryBuilder
				.withStarted(startedDt.toDate())
				.withEnded(endedDt.toDate())
				.withDuration(duration.longValue())
				.withMessage(message)
				.build();

		assertThat(converter.convert(entry))
				.isEqualTo("Code [uniqueCode] Status [SUCCESS] " +
						"Started [" + started +
						"] Ended [" + ended +
						"] Duration [" + duration + "]ms Message [" + message + "]");
	}

	@Test
	public void shouldHandleNulls()
	{
		final MonitorHistoryEntryActionData entry = entryBuilder.build();

		assertThat(converter.convert(entry))
				.isEqualTo("Code [uniqueCode] Status [SUCCESS] Started [null] Ended [null] Duration [null]ms Message [null]");
	}
}