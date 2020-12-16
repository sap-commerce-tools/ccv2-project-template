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
package de.hybris.platform.cloud.commons.spring.metadata.impl;

import com.google.common.collect.ImmutableMap;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.model.MetadataStoreEntryModel;
import de.hybris.platform.cloud.commons.spring.integration.metadata.HybrisMetadataStore;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.integration.support.locks.DefaultLockRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class HybrisMetadataStoreTest
{
	private static final long LOCK_TTL = 90000;
	private static final String REGION = "region";
	private static final String KEY = "key";
	private static final String VALUE = "value";
	private static final String CURRENT_VALUE = "currentValue";

	private HybrisMetadataStore store;

	@Mock
	private ModelService modelService;

	@Mock
	private GenericDao<MetadataStoreEntryModel> metadataStoreDao;

	@Mock
	private MetadataStoreEntryModel newEntry;

	@Mock
	private MetadataStoreEntryModel currentEntry;

	private final List<MetadataStoreEntryModel> results = new ArrayList<>();


	@Before
	public void setUp()
	{
		store = new HybrisMetadataStore(modelService, metadataStoreDao, new DefaultLockRegistry(), LOCK_TTL, REGION);

		given(modelService.create(MetadataStoreEntryModel.class)).willReturn(newEntry);

		given(metadataStoreDao.find(ImmutableMap.of("region", REGION, "key", KEY))).willReturn(results);

		given(currentEntry.getValue()).willReturn(CURRENT_VALUE);

	}

	@Test
	public void whenCallPut_thenCreateNewRecord()
	{
		store.put(KEY, VALUE);

		verifyNewRecordCreated();
	}

	private void verifyNewRecordCreated()
	{
		final InOrder order = inOrder(modelService, newEntry);
		order.verify(newEntry).setRegion(REGION);
		order.verify(newEntry).setKey(KEY);
		order.verify(newEntry).setValue(VALUE);
		order.verify(modelService).save(newEntry);
	}

	@Test
	public void whenCallPutIfAbsent_andNoExistingRecord_thenCreateNewRecordAndReturnNull()
	{
		assertThat(store.putIfAbsent(KEY, VALUE)).isNull();

		verifyNewRecordCreated();
	}

	@Test
	public void whenCallPutIfAbsent_andExistingRecord_thenReturnCurrentRecordValue()
	{
		results.add(currentEntry);

		assertThat(store.putIfAbsent(KEY, VALUE)).isEqualTo(CURRENT_VALUE);

		verify(modelService, never()).create(MetadataStoreEntryModel.class);
	}

	@Test
	public void whenCallGet_andNoExistingRecord_thenReturnNull()
	{
		assertThat(store.get(KEY)).isNull();
	}

	@Test
	public void whenCallGet_andExistingRecord_thenReturnCurrentRecordValue()
	{
		results.add(currentEntry);

		assertThat(store.get(KEY)).isEqualTo(CURRENT_VALUE);
	}

	@Test
	public void whenCallRemove_andNoExistingRecord_thenReturnNull()
	{
		assertThat(store.remove(KEY)).isNull();
	}

	@Test
	public void whenCallRemove_andExistingRecord_thenRemoveCurrentRecordAndReturnItsValue()
	{
		results.add(currentEntry);

		assertThat(store.remove(KEY)).isEqualTo(CURRENT_VALUE);

		verify(modelService).remove(currentEntry);
	}

	@Test
	public void whenCallReplace_andNoExistsRecord_thenReturnFalse()
	{
		assertThat(store.replace(KEY, CURRENT_VALUE, VALUE)).isFalse();
	}

	@Test
	public void whenCallReplace_andExistsRecord_andOldValueDoesNotMatchItsCurrentValue_thenReturnFalse()
	{
		results.add(currentEntry);

		assertThat(store.replace(KEY, "dodgyValue", VALUE)).isFalse();

		verify(modelService, never()).save(currentEntry);
	}

	@Test
	public void whenCallReplace_andExistsRecord_andOldValueDoesMatchItsCurrentValue_thenReturnFalse()
	{
		results.add(currentEntry);

		assertThat(store.replace(KEY, CURRENT_VALUE, VALUE)).isTrue();

		final InOrder order = inOrder(modelService, currentEntry);
		order.verify(currentEntry).setValue(VALUE);
		order.verify(modelService).save(currentEntry);
	}

}
