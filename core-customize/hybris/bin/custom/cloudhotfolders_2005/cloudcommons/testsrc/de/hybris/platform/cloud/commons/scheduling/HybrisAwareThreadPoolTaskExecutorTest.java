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
package de.hybris.platform.cloud.commons.scheduling;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.Tenant;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class HybrisAwareThreadPoolTaskExecutorTest
{
	private static final String BEAN_NAME = "beanName";

	private static final ThreadFactory THREAD_FACTORY = mock(ThreadFactory.class);
	private static final Tenant TENANT = mock(Tenant.class);
	private static final int QUEUE_CAPACITY = 2;
	private static final int CORE_POOL_SIZE = 3;
	private static final int MAX_POOL_SIZE = 5;
	private static final Long KEEP_ALIVE_SECS = 5L;
	private static final RejectedExecutionHandler REJ_HANDLER = new ThreadPoolExecutor.AbortPolicy();
	private static final int TERMINATION_TIMEOUT = 6;

	@Spy
	private HybrisAwareThreadPoolTaskExecutor executor = new HybrisAwareThreadPoolTaskExecutor();

	@Before
	public void setUp()
	{
		willReturn(TENANT).given(executor).getCurrentTenant();

		executor.setQueueCapacity(QUEUE_CAPACITY);
		executor.setCorePoolSize(CORE_POOL_SIZE);
		executor.setMaxPoolSize(MAX_POOL_SIZE);
		executor.setKeepAliveSeconds(KEEP_ALIVE_SECS.intValue());
		executor.setRejectedExecutionHandler(REJ_HANDLER);
		executor.setThreadFactory(THREAD_FACTORY);
		executor.setAwaitTerminationSeconds(TERMINATION_TIMEOUT);

		executor.afterPropertiesSet();

		executor.setBeanName(BEAN_NAME);
	}

	private ThreadPoolExecutor givenMockThreadPoolExecutor()
	{
		final ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class);
		willReturn(threadPoolExecutor)
				.given(executor)
				.createThreadPoolExecutor(eq(TENANT), eq(CORE_POOL_SIZE), eq(MAX_POOL_SIZE), eq(KEEP_ALIVE_SECS), eq(TimeUnit.SECONDS), any(), eq(THREAD_FACTORY), eq(REJ_HANDLER));

		return threadPoolExecutor;
	}

	@Test
	public void initialize_shouldSetThreadNamePrefixToNameOfBean()
	{
		HybrisAwareThreadPoolTaskExecutor local = new HybrisAwareThreadPoolTaskExecutor();
		local.setBeanName(BEAN_NAME);

		local.initialize();

		assertThat(local.getThreadNamePrefix()).isEqualTo(BEAN_NAME + "-");
	}

	@Test
	public void initialize_shouldCreateHybrisAwareThreadPoolExecutorCorrectly_LinkedQueue()
	{
		executor.initialize();

		final HybrisAwareThreadPoolExecutor threadPoolExecutor = Optional.of(executor.getThreadPoolExecutor())
				.filter(HybrisAwareThreadPoolExecutor.class::isInstance)
				.map(HybrisAwareThreadPoolExecutor.class::cast)
				.orElseThrow(() -> new IllegalStateException("threadPoolExecutor was not instanceof HybrisAwareThreadPoolExecutor"));


		final LinkedBlockingQueue<Runnable> expectedQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
		final HybrisAwareThreadPoolExecutor expected = new HybrisAwareThreadPoolExecutor(TENANT, CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECS,
				TimeUnit.SECONDS, expectedQueue, THREAD_FACTORY, REJ_HANDLER);

		assertThat(threadPoolExecutor).isEqualToComparingOnlyGivenFields(expected, "tenant", "corePoolSize", "maximumPoolSize", "keepAliveTime", "threadFactory", "handler", "allowCoreThreadTimeOut");

		final BlockingQueue<Runnable> actualQueue = threadPoolExecutor.getQueue();
		assertThat(actualQueue).isInstanceOf(expectedQueue.getClass());
		assertThat(actualQueue.remainingCapacity()).isEqualTo(expectedQueue.remainingCapacity());
	}

	@Test
	public void initialize_shouldCreateHybrisAwareThreadPoolExecutorCorrectly_SynchronousQueue()
	{
		executor.setQueueCapacity(0);
		executor.initialize();

		final HybrisAwareThreadPoolExecutor threadPoolExecutor = Optional.of(executor.getThreadPoolExecutor())
				.filter(HybrisAwareThreadPoolExecutor.class::isInstance)
				.map(HybrisAwareThreadPoolExecutor.class::cast)
				.orElseThrow(() -> new IllegalStateException("threadPoolExecutor was not instanceof HybrisAwareThreadPoolExecutor"));

		final SynchronousQueue<Runnable> expectedQueue = new SynchronousQueue<>();
		final HybrisAwareThreadPoolExecutor expected = new HybrisAwareThreadPoolExecutor(TENANT, CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECS,
				TimeUnit.SECONDS, expectedQueue, THREAD_FACTORY, REJ_HANDLER);

		assertThat(threadPoolExecutor).isEqualToComparingOnlyGivenFields(expected, "tenant", "corePoolSize", "maximumPoolSize", "keepAliveTime", "threadFactory", "handler", "allowCoreThreadTimeOut");

		final BlockingQueue<Runnable> actualQueue = threadPoolExecutor.getQueue();
		assertThat(actualQueue).isInstanceOf(expectedQueue.getClass());
		assertThat(actualQueue.remainingCapacity()).isEqualTo(expectedQueue.remainingCapacity());
	}

	@Test
	public void initialize_givenIsInitialized_shouldMarkAsRunning()
	{
		assertThat(executor.isRunning()).isFalse();

		executor.initialize();

		assertThat(executor.isRunning()).isTrue();
	}

	@Test
	public void shutdown_givenIsNotInitialized_shouldNotNullPointer()
	{
		assertThat(executor.isRunning()).isFalse();

		executor.shutdown();
	}

	@Test
	public void shutdown_givenIsInitialized_andShouldWaitForShutdown_whenShutdown_thenShouldWaitForExecutorServiceShutdown()
	{
		givenMockThreadPoolExecutor();

		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();

		executor.shutdown();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		verify(threadPoolExecutor).shutdown();
	}

	@Test
	public void shutdown_givenIsInitialized_andDoNotWaitForShutdown_whenShutdown_thenShouldExecutorServiceShutdownNow()
	{
		givenMockThreadPoolExecutor();

		executor.setWaitForTasksToCompleteOnShutdown(false);
		executor.initialize();

		executor.shutdown();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		verify(threadPoolExecutor).shutdownNow();
	}

	@Test
	public void shutdown_givenIsInitialized_andShouldAwaitTermination_whenShutdown_thenShouldAwaitTermination() throws InterruptedException
	{
		givenMockThreadPoolExecutor();

		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();

		executor.shutdown();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		verify(threadPoolExecutor).awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
	}

	@Test
	public void shutdown_givenIsInitialized_andShouldNotAwaitTermination_whenShutdown_thenShouldNotAwaitTermination() throws InterruptedException
	{
		executor.setAwaitTerminationSeconds(0);
		givenMockThreadPoolExecutor();

		executor.initialize();

		executor.shutdown();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		verify(threadPoolExecutor, never()).awaitTermination(anyInt(), any());
	}

	@Test
	public void setCorePoolSize_givenInitialized_thenShouldUpdateExecutor()
	{
		executor.initialize();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(CORE_POOL_SIZE);

		executor.setCorePoolSize(4);
		assertThat(executor.getCorePoolSize()).isEqualTo(4);
		assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(4);
	}

	@Test
	public void setMaxPoolSize_givenInitialized_thenShouldUpdateExecutor()
	{
		executor.initialize();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(CORE_POOL_SIZE);

		executor.setMaxPoolSize(100);
		assertThat(executor.getMaxPoolSize()).isEqualTo(100);
		assertThat(threadPoolExecutor.getMaximumPoolSize()).isEqualTo(100);
	}

	@Test
	public void setKeepAliveSeconds_givenInitialized_thenShouldUpdateExecutor()
	{
		executor.initialize();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(CORE_POOL_SIZE);

		executor.setKeepAliveSeconds(100);
		assertThat(executor.getKeepAliveSeconds()).isEqualTo(100);
		assertThat(threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(100);
	}

	@Test
	public void givenAllowCoreThreadTimeOut_whenIntialize_thenShouldAllowCoreThreadTimeOut()
	{
		executor.setAllowCoreThreadTimeOut(true);

		executor.initialize();

		final ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
		assertThat(threadPoolExecutor.allowsCoreThreadTimeOut()).isTrue();
	}

	@Test
	public void getPoolSize_shouldReturnPoolSizeOfThreadPoolExecutor()
	{
		final ThreadPoolExecutor threadPoolExecutor = givenMockThreadPoolExecutor();
		given(threadPoolExecutor.getPoolSize()).willReturn(100);

		executor.initialize();

		assertThat(executor.getPoolSize()).isEqualTo(100);
	}

	@Test
	public void getActiveCount_shouldReturnActiveCountOfThreadPoolExecutor()
	{
		final ThreadPoolExecutor threadPoolExecutor = givenMockThreadPoolExecutor();
		given(threadPoolExecutor.getActiveCount()).willReturn(100);

		executor.initialize();

		assertThat(executor.getActiveCount()).isEqualTo(100);
	}

	@Test
	public void execute_shouldDelegateToThreadPoolExecutor()
	{
		final ThreadPoolExecutor threadPoolExecutor = givenMockThreadPoolExecutor();

		executor.initialize();

		final Runnable runnable = mock(Runnable.class);
		executor.execute(runnable);

		verify(threadPoolExecutor).execute(runnable);
	}

	@Test
	public void executeWithTimeout_shouldDelegateToThreadPoolExecutor()
	{
		final ThreadPoolExecutor threadPoolExecutor = givenMockThreadPoolExecutor();

		executor.initialize();

		final Runnable runnable = mock(Runnable.class);
		executor.execute(runnable, 1L);

		verify(threadPoolExecutor).execute(runnable);
	}

	@Test
	public void submitRunnable_shouldDelegateToThreadPoolExecutor()
	{
		final ThreadPoolExecutor threadPoolExecutor = givenMockThreadPoolExecutor();

		executor.initialize();

		final Runnable runnable = mock(Runnable.class);
		executor.submit(runnable);

		verify(threadPoolExecutor).submit(runnable);
	}

	@Test
	public void submitCallable_shouldDelegateToThreadPoolExecutor()
	{
		final ThreadPoolExecutor threadPoolExecutor = givenMockThreadPoolExecutor();

		executor.initialize();

		final Callable<?> callable = mock(Callable.class);
		executor.submit(callable);

		verify(threadPoolExecutor).submit(callable);
	}

	@Test
	public void prefersShortLivedTasks_shouldReturnTrue()
	{
		assertThat(executor.prefersShortLivedTasks()).isTrue();
	}

	@Test
	public void start_givenNotRunning_thenShouldInitialize()
	{
		assertThat(executor.isRunning()).isFalse();

		executor.start();

		verify(executor).initialize();
		assertThat(executor.isRunning()).isTrue();
	}

	@Test
	public void start_givenRunning_thenShouldNotInitializeAgain()
	{
		executor.start();
		assertThat(executor.isRunning()).isTrue();

		executor.start();
		assertThat(executor.isRunning()).isTrue();
		verify(executor, times(1)).initialize();
	}

	@Test
	public void stop_givenNotRunning_thenShouldNotShutdown()
	{
		assertThat(executor.isRunning()).isFalse();

		executor.stop();

		verify(executor, never()).shutdown();
	}

	@Test
	public void stop_givenRunning_thenShouldShutdown()
	{
		executor.start();
		assertThat(executor.isRunning()).isTrue();

		executor.stop();

		verify(executor).shutdown();
	}

	@Test
	public void stopWithRunnable_shouldStopAndThenRunRunnable()
	{
		final Runnable runnable = mock(Runnable.class);

		executor.stop(runnable);

		verify(executor).stop();
		verify(runnable).run();
	}

}