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
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistoryFactory;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.commons.services.monitor.database.dao.MonitorHistoryDataDao;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DatabaseMonitorHistoryRestorerTest
{
	private static final SystemArea AREA = SystemArea.INTEGRATION;
	private static final String KEY = "key";

	private DatabaseMonitorHistoryRestorer monitor = new DatabaseMonitorHistoryRestorer();

	@Mock
	private MonitorHistoryDataDao monitoryHistoryDao;

	@Mock
	private Converter<MonitorHistoryDataModel, MonitorHistoryData> historyReverseConverter;

	private MonitorHistoryFactory historyFactory = new MonitorHistoryFactory();

	private MonitorHistoryData historyData = new MonitorHistoryData();

	@Mock
	private MonitorHistoryDataModel historyModel;

	@Before
	public void setUp()
	{
		monitor.setMonitoryHistoryDao(monitoryHistoryDao);
		monitor.setHistoryReverseConverter(historyReverseConverter);
		monitor.setHistoryFactory(historyFactory);
	}

	@Test
	public void restoreShouldConvertExistingRecordBackIntoHistoryData()
	{
		given(monitoryHistoryDao.find(AREA, KEY)).willReturn(Collections.singletonList(historyModel));
		given(historyReverseConverter.convert(historyModel)).willReturn(historyData);

		assertThat(monitor.restore(AREA, KEY)).isEqualTo(historyData);
	}

	@Test
	public void restoreShouldConvertFirstExistingRecordBackIntoHistoryData()
	{
		given(monitoryHistoryDao.find(AREA, KEY)).willReturn(Arrays.asList(historyModel, mock(MonitorHistoryDataModel.class)));
		given(historyReverseConverter.convert(historyModel)).willReturn(historyData);

		assertThat(monitor.restore(AREA, KEY)).isEqualTo(historyData);
	}

	@Test
	public void restoreShouldCreateNewHistoryDataWhereExistingOneNotFound()
	{
		given(monitoryHistoryDao.find(AREA, KEY)).willReturn(Collections.emptyList());

		historyData.setSystemArea(AREA);
		historyData.setKey(KEY);
		historyData.setStatus(Status.IN_PROGRESS);
		historyData.setEntries(Collections.emptyList());

		assertThat(monitor.restore(AREA, KEY)).isEqualToComparingFieldByField(historyData);
	}

}