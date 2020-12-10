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
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryData;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryEntryData;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.services.monitor.logging.LogData;

import java.util.Collections;

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
public class MonitorHistoryDataConverterTest
{
	private static final String ENTRY_STRING = "entry String";
	private static final String KEY = "key";
	private static final String EXCEPTION_MESSAGE = "exceptionMessage";

	private MonitorHistoryDataConverter converter = new MonitorHistoryDataConverter();

	@Mock
	private Converter<MonitorHistoryEntryData, String> entryConverter;

	@Mock
	private MonitorHistoryEntryData entry;

	private MonitorHistoryData history = new MonitorHistoryData();

	@Mock
	private Throwable exception;

	@Before
	public void setUp()
	{
		converter.setEntryConverter(entryConverter);

		history.setSystemArea(SystemArea.INTEGRATION);
		history.setKey(KEY);
		history.setStatus(Status.FAILURE);
		history.setEntries(Collections.singletonList(entry));
		history.setExceptionMessage(EXCEPTION_MESSAGE);
		history.setException(exception);

		given(entryConverter.convert(entry)).willReturn(ENTRY_STRING);
	}

	@Test
	public void shouldConvertDataIntoRequiredLogData()
	{
		final LogData expected = LogData.builder()
				.withProcessedMessage(SystemArea.INTEGRATION + ":" + KEY + " Status [" + Status.FAILURE + "]" + System.lineSeparator() + ENTRY_STRING)
				.withExceptionMessage(EXCEPTION_MESSAGE)
				.withException(exception)
				.build();

		final LogData actual = converter.convert(history);

		assertThat(actual).isEqualToComparingFieldByField(expected);
	}


}