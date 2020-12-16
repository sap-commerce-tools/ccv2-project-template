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
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryFactory;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.core.PK;

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
public class MonitorHistoryReverseConverterTest
{
	private static final String EXCEPTION_MESSAGE = "exceptionMessage";
	private static final String KEY = "key";
	private static final MonitorSystemArea AREA = MonitorSystemArea.INTEGRATION;
	private static final MonitorStatus STATUS = MonitorStatus.IN_PROGRESS;
	private static final SystemArea EXPECTED_AREA = SystemArea.INTEGRATION;
	private static final Status EXPECTED_STATUS = Status.IN_PROGRESS;
	private PK pk = PK.fromLong(1L);

	private MonitorHistoryReverseConverter converter = new MonitorHistoryReverseConverter();

	private MonitorHistoryFactory historyFactory = new MonitorHistoryFactory();

	@Mock
	private Converter<MonitorHistoryEntryDataModel, MonitorHistoryEntryData> entryReverseConverter;

	@Mock
	private MonitorHistoryDataModel source;

	@Mock
	private MonitorHistoryEntryDataModel entryModel;

	private MonitorHistoryEntryData entryData = MonitorHistoryEntryData.builder().build();

	@Before
	public void setUp()
	{
		converter.setHistoryFactory(historyFactory);
		converter.setEntryReverseConverter(entryReverseConverter);

		given(source.getSystemArea()).willReturn(AREA);
		given(source.getKey()).willReturn(KEY);
		given(source.getPk()).willReturn(pk);
		given(source.getStatus()).willReturn(STATUS);
		given(source.getExceptionMessage()).willReturn(EXCEPTION_MESSAGE);
		given(source.getEntries()).willReturn(Collections.singletonList(entryModel));

		given(entryReverseConverter.convert(entryModel)).willReturn(entryData);
	}

	@Test
	public void shouldConvertDataCorrectly()
	{
		final MonitorHistoryData expected= new MonitorHistoryData();
		expected.setPk(pk);
		expected.setSystemArea(EXPECTED_AREA);
		expected.setKey(KEY);
		expected.setStatus(EXPECTED_STATUS);
		expected.setStatusWhenResumed(EXPECTED_STATUS);
		expected.setExceptionMessage(EXCEPTION_MESSAGE);
		expected.setEntries(Collections.singletonList(entryData));

		assertThat(converter.convert(source)).isEqualToComparingFieldByField(expected);
	}

	@Test
	public void shouldHandleNullEntries()
	{
		given(source.getEntries()).willReturn(null);

		final MonitorHistoryData expected= new MonitorHistoryData();
		expected.setPk(pk);
		expected.setSystemArea(EXPECTED_AREA);
		expected.setKey(KEY);
		expected.setStatus(EXPECTED_STATUS);
		expected.setStatusWhenResumed(EXPECTED_STATUS);
		expected.setExceptionMessage(EXCEPTION_MESSAGE);
		expected.setEntries(Collections.emptyList());

		assertThat(converter.convert(source)).isEqualToComparingFieldByField(expected);
	}
}