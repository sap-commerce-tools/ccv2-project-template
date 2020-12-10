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
package de.hybris.platform.cloud.commons.services.monitor.database.dao.impl;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.enums.MonitorSystemArea;
import de.hybris.platform.cloud.commons.model.MonitorHistoryDataModel;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultMonitorHistoryDataDaoTest
{
	private static final SystemArea AREA = SystemArea.INTEGRATION;
	private static final MonitorSystemArea EXPECTED_AREA = MonitorSystemArea.INTEGRATION;
	private static final String KEY = "ABC";

	private static final String SQL = String.format("SELECT {c:pk} FROM {%s AS c} WHERE {c:%s}=?%s AND {c:%s}=?%s ",
			MonitorHistoryDataModel._TYPECODE,
			MonitorHistoryDataModel.SYSTEMAREA, MonitorHistoryDataModel.SYSTEMAREA,
			MonitorHistoryDataModel.KEY, MonitorHistoryDataModel.KEY
	);

	private DefaultMonitorHistoryDataDao dao = new DefaultMonitorHistoryDataDao();

	@Mock
	private FlexibleSearchService flexibleSearchService;

	@Mock
	private SearchResult<MonitorHistoryDataModel> searchResult;

	@Mock
	private MonitorHistoryDataModel historyModel;

	@Before
	public void setUp()
	{
		dao.setFlexibleSearchService(flexibleSearchService);

		given(flexibleSearchService.<MonitorHistoryDataModel>search(isA(FlexibleSearchQuery.class))).willReturn(searchResult);

		given(searchResult.getResult()).willReturn(Collections.singletonList(historyModel));
	}

	@Test
	public void shouldSearchReturnResultFromSearch() {
		assertThat(dao.find(AREA, KEY)).isEqualTo(Collections.singletonList(historyModel));
	}

	@Test
	public void shouldSearchWithStringValueOfEnum() {
		dao.find(AREA, KEY);

		final ArgumentCaptor<FlexibleSearchQuery> arg = ArgumentCaptor.forClass(FlexibleSearchQuery.class);
		verify(flexibleSearchService).<MonitorHistoryDataModel>search(arg.capture());
		final FlexibleSearchQuery actual = arg.getValue();

		final FlexibleSearchQuery expected = new FlexibleSearchQuery(SQL);
		expected.addQueryParameters(ImmutableMap.<String, Object>of(MonitorHistoryDataModel.SYSTEMAREA, EXPECTED_AREA, MonitorHistoryDataModel.KEY, KEY));

		assertThat(actual).isEqualToComparingOnlyGivenFields(expected,  "queryParameters");
	}

}