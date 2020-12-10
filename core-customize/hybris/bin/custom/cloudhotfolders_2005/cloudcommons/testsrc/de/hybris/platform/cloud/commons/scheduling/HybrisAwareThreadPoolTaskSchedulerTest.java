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
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;

import static org.assertj.core.api.Assertions.assertThat;
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
public class HybrisAwareThreadPoolTaskSchedulerTest
{
	private static final String BEAN_NAME = "beanName";

	private static final ThreadFactory THREAD_FACTORY = mock(ThreadFactory.class);
	private static final Tenant TENANT = mock(Tenant.class);
	private static final int POOL_SIZE = 3;
	private static final RejectedExecutionHandler REJ_HANDLER = new ThreadPoolExecutor.AbortPolicy();
	private static final int TERMINATION_TIMEOUT = 6;

	@Spy
	private HybrisAwareThreadPoolTaskScheduler scheduler = new HybrisAwareThreadPoolTaskScheduler();

	@Before
	public void setUp()
	{
		willReturn(TENANT).given(scheduler).getCurrentTenant();

		scheduler.setRejectedExecutionHandler(REJ_HANDLER);
		scheduler.setThreadFactory(THREAD_FACTORY);
		scheduler.setAwaitTerminationSeconds(TERMINATION_TIMEOUT);
		scheduler.setPoolSize(POOL_SIZE);

		scheduler.afterPropertiesSet();

		scheduler.setBeanName(BEAN_NAME);
	}

	private ScheduledExecutorService givenMockScheduledExecutor()
	{
		final ScheduledExecutorService threadPoolExecutor = mock(ScheduledExecutorService.class);
		willReturn(threadPoolExecutor)
				.given(scheduler)
				.createExecutorService(eq(TENANT), eq(POOL_SIZE), eq(THREAD_FACTORY), eq(REJ_HANDLER));

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
	public void initialize_shouldCreateHybrisAwareThreadPoolExecutorCorrectly()
	{
		scheduler.initialize();

		final HybrisAwareScheduledThreadPoolExecutor threadPoolExecutor = Optional.of(scheduler.getScheduledExecutor())
				.filter(HybrisAwareScheduledThreadPoolExecutor.class::isInstance)
				.map(HybrisAwareScheduledThreadPoolExecutor.class::cast)
				.orElseThrow(() -> new IllegalStateException("scheduledExecutor was not instanceof HybrisAwareScheduledThreadPoolExecutor"));


		final HybrisAwareScheduledThreadPoolExecutor expected = new HybrisAwareScheduledThreadPoolExecutor(TENANT, POOL_SIZE, THREAD_FACTORY, REJ_HANDLER);

		assertThat(threadPoolExecutor).isEqualToComparingOnlyGivenFields(expected, "tenant", "poolSize", "threadFactory", "handler");
	}

	@Test
	public void initialize_givenIsInitialized_shouldMarkAsRunning()
	{
		assertThat(scheduler.isRunning()).isFalse();

		scheduler.initialize();

		assertThat(scheduler.isRunning()).isTrue();
	}

	@Test
	public void shutdown_givenIsNotInitialized_shouldNotNullPointer()
	{
		assertThat(scheduler.isRunning()).isFalse();

		scheduler.shutdown();
	}

	@Test
	public void shutdown_givenIsInitialized_andShouldWaitForShutdown_whenShutdown_thenShouldWaitForExecutorServiceShutdown()
	{
		givenMockScheduledExecutor();

		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.initialize();

		scheduler.shutdown();

		final ScheduledExecutorService threadPoolExecutor = scheduler.getScheduledExecutor();
		verify(threadPoolExecutor).shutdown();
	}

	@Test
	public void shutdown_givenIsInitialized_andDoNotWaitForShutdown_whenShutdown_thenShouldExecutorServiceShutdownNow()
	{
		givenMockScheduledExecutor();

		scheduler.setWaitForTasksToCompleteOnShutdown(false);
		scheduler.initialize();

		scheduler.shutdown();

		final ScheduledExecutorService threadPoolExecutor = scheduler.getScheduledExecutor();
		verify(threadPoolExecutor).shutdownNow();
	}

	@Test
	public void shutdown_givenIsInitialized_andShouldAwaitTermination_whenShutdown_thenShouldAwaitTermination() throws InterruptedException
	{
		givenMockScheduledExecutor();

		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.initialize();

		scheduler.shutdown();

		final ScheduledExecutorService threadPoolExecutor = scheduler.getScheduledExecutor();
		verify(threadPoolExecutor).awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
	}

	@Test
	public void shutdown_givenIsInitialized_andShouldNotAwaitTermination_whenShutdown_thenShouldNotAwaitTermination() throws InterruptedException
	{
		scheduler.setAwaitTerminationSeconds(0);
		givenMockScheduledExecutor();

		scheduler.initialize();

		scheduler.shutdown();

		final ScheduledExecutorService threadPoolExecutor = scheduler.getScheduledExecutor();
		verify(threadPoolExecutor, never()).awaitTermination(anyInt(), any());
	}

	@Test
	public void execute_shouldDelegateToThreadPoolExecutor()
	{
		final ScheduledExecutorService threadPoolExecutor = givenMockScheduledExecutor();

		scheduler.initialize();

		final DelegatingErrorHandlingRunnable runnable = mock(DelegatingErrorHandlingRunnable.class);
		scheduler.execute(runnable);

		verify(threadPoolExecutor).execute(runnable);
	}

	@Test
	public void executeWithTimeout_shouldDelegateToThreadPoolExecutor()
	{
		final ScheduledExecutorService threadPoolExecutor = givenMockScheduledExecutor();

		scheduler.initialize();

		final DelegatingErrorHandlingRunnable runnable = mock(DelegatingErrorHandlingRunnable.class);
		scheduler.execute(runnable, 1L);

		verify(threadPoolExecutor).execute(runnable);
	}

	@Test
	public void submitRunnable_shouldDelegateToThreadPoolExecutor()
	{
		final ScheduledExecutorService threadPoolExecutor = givenMockScheduledExecutor();

		scheduler.initialize();

		final DelegatingErrorHandlingRunnable runnable = mock(DelegatingErrorHandlingRunnable.class);
		scheduler.submit(runnable);

		verify(threadPoolExecutor).submit(runnable);
	}

	@Test
	public void submitCallable_shouldDelegateToThreadPoolExecutor()
	{
		final ScheduledExecutorService threadPoolExecutor = givenMockScheduledExecutor();

		scheduler.initialize();

		final Callable<?> callable = mock(Callable.class);
		scheduler.submit(callable);

		verify(threadPoolExecutor).submit(callable);
	}

	@Test
	public void prefersShortLivedTasks_shouldReturnTrue()
	{
		assertThat(scheduler.prefersShortLivedTasks()).isTrue();
	}

	@Test
	public void start_givenNotRunning_thenShouldInitialize()
	{
		assertThat(scheduler.isRunning()).isFalse();

		scheduler.start();

		verify(scheduler).initialize();
		assertThat(scheduler.isRunning()).isTrue();
	}

	@Test
	public void start_givenRunning_thenShouldNotInitializeAgain()
	{
		scheduler.start();
		assertThat(scheduler.isRunning()).isTrue();

		scheduler.start();
		assertThat(scheduler.isRunning()).isTrue();
		verify(scheduler, times(1)).initialize();
	}

	@Test
	public void stop_givenNotRunning_thenShouldNotShutdown()
	{
		assertThat(scheduler.isRunning()).isFalse();

		scheduler.stop();

		verify(scheduler, never()).shutdown();
	}

	@Test
	public void stop_givenRunning_thenShouldShutdown()
	{
		scheduler.start();
		assertThat(scheduler.isRunning()).isTrue();

		scheduler.stop();

		verify(scheduler).shutdown();
	}

	@Test
	public void stopWithRunnable_shouldStopAndThenRunRunnable()
	{
		final Runnable runnable = mock(Runnable.class);

		scheduler.stop(runnable);

		verify(scheduler).stop();
		verify(runnable).run();
	}
}