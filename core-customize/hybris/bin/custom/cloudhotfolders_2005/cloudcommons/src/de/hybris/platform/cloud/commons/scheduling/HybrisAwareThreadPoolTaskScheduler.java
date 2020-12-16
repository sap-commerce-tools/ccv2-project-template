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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * {@link ThreadPoolTaskScheduler} implementation to ensure threads are aware of Hybris requirements
 * e.g. set with a current tenant and an active Session ready for running queries
 *
 */
public class HybrisAwareThreadPoolTaskScheduler extends ThreadPoolTaskScheduler implements SmartLifecycleRole
{
	private volatile boolean autoStartup = true;
	private volatile int phase = 0;
	private volatile String role = null;
	private volatile boolean initialized = false;

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

	@Override
	public void initialize()
	{
		if (!this.initialized)
		{
			super.initialize();
			initialized = true;
		}
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void shutdown()
	{
		if (this.initialized)
		{
			super.shutdown();
			this.initialized = false;
		}
	}

	@Override
	protected ScheduledExecutorService createExecutor(final int poolSize, final ThreadFactory threadFactory, final RejectedExecutionHandler rejectedExecutionHandler)
	{
		return createExecutorService(this.tenant, poolSize, threadFactory, rejectedExecutionHandler);
	}

	protected ScheduledExecutorService createExecutorService(final Tenant tenant, final int poolSize, final ThreadFactory threadFactory,
	                                                         final RejectedExecutionHandler rejectedExecutionHandler)
	{
		return new HybrisAwareScheduledThreadPoolExecutor(tenant, poolSize, threadFactory, rejectedExecutionHandler);
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

	@Override
	public void stop(final Runnable runnable)
	{
		stop();
		runnable.run();
	}

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
	public boolean isRunning()
	{
		return this.initialized;
	}

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
