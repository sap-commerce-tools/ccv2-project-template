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
package de.hybris.platform.cloud.commons.spring.integration.support.locks.database;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.model.ApplicationResourceLockModel;
import de.hybris.platform.cloud.commons.spring.integration.support.locks.database.dao.ApplicationResourceLockDao;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class HybrisLockRepositoryTest
{
	private static final int TTL = 250;
	private static final String REGION = "region";
	private static final int CLUSTER_ID = 0;
	public static final String LOCK_KEY = "lockKey";

	@Spy
	private final HybrisLockRepository lockRepository = new HybrisLockRepository();

	@Mock
	private ModelService modelService;

	@Mock
	private ApplicationResourceLockDao applicationResourceLockDao;

	@Mock
	private ApplicationResourceLockModel lock;

	@Mock
	private ApplicationResourceLockModel newLock;

	private final Date expiryDate = new Date();

	@Before
	public void setUp()
	{
		lockRepository.setModelService(modelService);
		lockRepository.setApplicationResourceLockDao(applicationResourceLockDao);
		lockRepository.setRegion(REGION);
		lockRepository.setTtl(TTL);

		willReturn(true).given(modelService).isAttached(lock);

		willReturn(CLUSTER_ID).given(lockRepository).getClusterId();
		willReturn(expiryDate).given(lockRepository).calculateExpiryDate(TTL);

		given(applicationResourceLockDao.getLocks(REGION, CLUSTER_ID)).willReturn(Collections.singletonList(lock));

		given(modelService.create(ApplicationResourceLockModel.class)).willReturn(newLock);
	}

	@Test
	public void start_shouldMarkAsRunning()
	{
		assertThat(lockRepository.isRunning()).isFalse();

		lockRepository.start();

		assertThat(lockRepository.isRunning()).isTrue();
	}

	@Test
	public void stop_givenIsNotRunning_thenShouldNotDeleteLocks()
	{
		assertThat(lockRepository.isRunning()).isFalse();

		lockRepository.stop();

		verify(applicationResourceLockDao, never()).getLocks(REGION, CLUSTER_ID);
	}

	@Test
	public void stop_givenIsRunning_thenShouldDeleteLocks()
	{
		lockRepository.start();
		assertThat(lockRepository.isRunning()).isTrue();

		lockRepository.stop();

		verify(modelService).remove(lock);
	}

	@Test
	public void stop_givenIsRunning_thenShouldMarkAsNotRunning()
	{
		lockRepository.start();
		assertThat(lockRepository.isRunning()).isTrue();

		lockRepository.stop();

		assertThat(lockRepository.isRunning()).isFalse();
	}

	@Test
	public void acquire_shouldDeleteExpiredLocksBeforeAttemptToGetLock()
	{
		given(applicationResourceLockDao.getLocks(LOCK_KEY, REGION, expiryDate)).willReturn(Collections.singletonList(lock));

		lockRepository.acquire(LOCK_KEY);

		final InOrder order = inOrder(modelService, applicationResourceLockDao);
		order.verify(modelService).remove(lock);
		order.verify(applicationResourceLockDao).getLock(LOCK_KEY, REGION, CLUSTER_ID);
	}

	@Test
	public void acquire_shouldCreateNewLockWhereOneIsNotFound()
	{
		assertThat(lockRepository.acquire(LOCK_KEY)).isTrue();

		final InOrder order = inOrder(modelService, newLock);
		order.verify(modelService).enableTransactions();
		order.verify(newLock).setLockKey(LOCK_KEY);
		order.verify(newLock).setRegion(REGION);
		order.verify(newLock).setClusterId(CLUSTER_ID);
		order.verify(newLock).setTimestamp(isA(Date.class));
		order.verify(modelService).save(newLock);
		order.verify(modelService).clearTransactionsSettings();
	}

	@Test
	public void acquire_shouldUpdateLockWhereIsFound()
	{
		given(applicationResourceLockDao.getLock(LOCK_KEY, REGION, CLUSTER_ID)).willReturn(lock);

		assertThat(lockRepository.acquire(LOCK_KEY)).isTrue();

		final InOrder order = inOrder(modelService, lock);
		order.verify(modelService).enableTransactions();
		order.verify(lock).setTimestamp(isA(Date.class));
		order.verify(modelService).save(lock);
		order.verify(modelService).clearTransactionsSettings();
	}

	@Test
	public void isAcquired_shouldDeleteExpiredLocksBeforeAttemptToGetLock()
	{
		given(applicationResourceLockDao.getLocks(LOCK_KEY, REGION, expiryDate)).willReturn(Collections.singletonList(lock));

		lockRepository.isAcquired(LOCK_KEY);

		final InOrder order = inOrder(modelService, applicationResourceLockDao);
		order.verify(modelService).remove(lock);
		order.verify(applicationResourceLockDao).getLock(LOCK_KEY, REGION, CLUSTER_ID, expiryDate);
	}

	@Test
	public void isAcquired_shouldReturnFalseWhenNoLock()
	{
		given(applicationResourceLockDao.getLock(LOCK_KEY, REGION, CLUSTER_ID, expiryDate)).willReturn(null);

		assertThat(lockRepository.isAcquired(LOCK_KEY)).isFalse();
	}

	@Test
	public void isAcquired_shouldReturnTrueWhenHasLock()
	{
		given(applicationResourceLockDao.getLock(LOCK_KEY, REGION, CLUSTER_ID, expiryDate)).willReturn(null);

		assertThat(lockRepository.isAcquired(LOCK_KEY)).isFalse();
	}

	@Test
	public void delete_shouldDoNothingWhenHasNoLock()
	{
		given(applicationResourceLockDao.getLock(LOCK_KEY, REGION, CLUSTER_ID)).willReturn(null);

		lockRepository.delete(LOCK_KEY);

		verify(modelService, never()).remove(any());
	}
	@Test
	public void delete_shouldRemoveFoundLock()
	{
		given(applicationResourceLockDao.getLock(LOCK_KEY, REGION, CLUSTER_ID)).willReturn(lock);

		lockRepository.delete(LOCK_KEY);

		verify(modelService).remove(lock);
	}
}