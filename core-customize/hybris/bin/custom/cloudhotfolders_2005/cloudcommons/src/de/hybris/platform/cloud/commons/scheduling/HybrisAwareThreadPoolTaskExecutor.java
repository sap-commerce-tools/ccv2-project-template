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

import de.hybris.platform.cloud.commons.spring.context.SmartLifecycleRole;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.Tenant;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import java.util.concurrent.*;

/**
 * {@link ThreadPoolTaskExecutor} implementation to ensure threads are aware of Hybris requirements
 * e.g. set with a current tenant and an active Session ready for running queries
 *
 */
public class HybrisAwareThreadPoolTaskExecutor extends ThreadPoolTaskExecutor implements SmartLifecycleRole
{
	private volatile boolean autoStartup = true;
	private volatile int phase = 0;
	private volatile String role = null;
	private volatile boolean initialized = false;

	private final Object poolSizeMonitor = new Object();

	private boolean allowCoreThreadTimeOut = false;

	private int queueCapacity = Integer.MAX_VALUE;

	private ThreadPoolExecutor threadPoolExecutor;

	private Tenant tenant;

	private String beanName;

	@Override
	public void afterPropertiesSet()
	{
		this.tenant = getCurrentTenant();
	}

	protected Tenant getCurrentTenant()
	{
		return Registry.getCurrentTenant();
	}

	//<editor-fold desc="overrides of ThreadPoolTaskExecutor">

	@Override
	public void initialize()
	{
		if (!initialized)
		{
			super.initialize();
			initialized = true;
		}
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void shutdown()
	{
		if (initialized)
		{
			super.shutdown();
			initialized = false;
		}
	}

	/**
	 * Set the ThreadPoolExecutor's core pool size.
	 * Default is 1.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	@Override
	public void setCorePoolSize(final int corePoolSize)
	{
		synchronized (this.poolSizeMonitor)
		{
			super.setCorePoolSize(corePoolSize);
			if (this.threadPoolExecutor != null)
			{
				this.threadPoolExecutor.setCorePoolSize(corePoolSize);
			}
		}
	}

	/**
	 * Set the ThreadPoolExecutor's maximum pool size.
	 * Default is <code>Integer.MAX_VALUE</code>.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	@Override
	public void setMaxPoolSize(final int maxPoolSize)
	{
		synchronized (this.poolSizeMonitor)
		{
			super.setMaxPoolSize(maxPoolSize);
			if (this.threadPoolExecutor != null)
			{
				this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
			}
		}
	}

	/**
	 * Set the ThreadPoolExecutor's keep-alive seconds.
	 * Default is 60.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	@Override
	public void setKeepAliveSeconds(final int keepAliveSeconds)
	{
		synchronized (this.poolSizeMonitor)
		{
			super.setKeepAliveSeconds(keepAliveSeconds);
			if (this.threadPoolExecutor != null)
			{
				this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * Specify whether to allow core threads to time out. This enables dynamic
	 * growing and shrinking even in combination with a non-zero queue (since
	 * the max pool size will only grow once the queue is full).
	 * <p>Default is "false". Note that this feature is only available on Java 6
	 * or above. On Java 5, consider switching to the backport-concurrent
	 * version of ThreadPoolTaskExecutor which also supports this feature.
	 * @see ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	@Override
	public void setAllowCoreThreadTimeOut(final boolean allowCoreThreadTimeOut)
	{
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
	 * Default is <code>Integer.MAX_VALUE</code>.
	 * <p>Any positive value will lead to a LinkedBlockingQueue instance;
	 * any other value will lead to a SynchronousQueue instance.
	 * @see LinkedBlockingQueue
	 * @see SynchronousQueue
	 */
	@Override
	public void setQueueCapacity(final int queueCapacity)
	{
		this.queueCapacity = queueCapacity;
	}

	/**
	 * Return the underlying ThreadPoolExecutor for native access.
	 * @return the underlying ThreadPoolExecutor (never <code>null</code>)
	 * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
	 */
	@Override
	public ThreadPoolExecutor getThreadPoolExecutor()
	{
		Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
		return this.threadPoolExecutor;
	}

	/**
	 * Return the current pool size.
	 * @see ThreadPoolExecutor#getPoolSize()
	 */
	@Override
	public int getPoolSize()
	{
		return getThreadPoolExecutor().getPoolSize();
	}

	/**
	 * Return the number of currently active threads.
	 * @see ThreadPoolExecutor#getActiveCount()
	 */
	@Override
	public int getActiveCount()
	{
		return getThreadPoolExecutor().getActiveCount();
	}

	@Override
	public void execute(final Runnable task, final long startTimeout)
	{
		execute(task);
	}

	@Override
	protected ExecutorService initializeExecutor(final ThreadFactory threadFactory, final RejectedExecutionHandler rejectedExecutionHandler)
	{
		final BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);

		final ThreadPoolExecutor executor =
				createThreadPoolExecutor(this.tenant, getCorePoolSize(), getMaxPoolSize(), getKeepAliveSeconds(), TimeUnit.SECONDS,
						queue, threadFactory, rejectedExecutionHandler);

		if (this.allowCoreThreadTimeOut)
		{
			executor.allowCoreThreadTimeOut(true);
		}

		this.threadPoolExecutor = executor;
		return executor;
	}

	// Suppress Sonar warnings - Extension of a parent constructor
	@SuppressWarnings("squid:S00107")
	protected ThreadPoolExecutor createThreadPoolExecutor(final Tenant tenant,
														  final int corePoolSize,
														  final int maximumPoolSize,
														  final long keepAliveTime,
	                                                      final TimeUnit unit,
														  final BlockingQueue<Runnable> workQueue,
														  final ThreadFactory threadFactory,
	                                                      final RejectedExecutionHandler handler)
	{
		return new HybrisAwareThreadPoolExecutor(tenant, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	//</editor-fold>

	//<editor-fold desc="Implementation of SmartLifecycleRole">

	@Override
	public void start()
	{
		if (!this.isRunning())
		{
			this.initialize();
		}
	}

	@Override
	public void stop()
	{
		if (this.isRunning())
		{
			this.shutdown();
		}
	}

	@Override
	public void stop(final Runnable runnable)
	{
		stop();
		runnable.run();
	}

	@Override
	public boolean isRunning()
	{
		return this.initialized;
	}

	@Override
	public String getRole()
	{
		return this.role;
	}

	public void setRole(final String role)
	{
		this.role = role;
	}

	@Override
	public boolean isAutoStartup()
	{
		return this.autoStartup;
	}

	public void setAutoStartup(final boolean autoStartup)
	{
		this.autoStartup = autoStartup;
	}

	@Override
	public int getPhase()
	{
		return this.phase;
	}

	public void setPhase(final int phase)
	{
		this.phase = phase;
	}

	//</editor-fold>

	@Override
	public void setBeanName(final String name)
	{
		super.setBeanName(name);
		this.beanName = name;
	}

	@Override
	public String toString()
	{
		return beanName;
	}
}
