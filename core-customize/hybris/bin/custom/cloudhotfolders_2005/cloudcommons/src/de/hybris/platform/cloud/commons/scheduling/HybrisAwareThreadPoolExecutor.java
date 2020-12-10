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

import de.hybris.platform.core.Registry;
import de.hybris.platform.core.Tenant;
import de.hybris.platform.core.threadregistry.OperationInfo;
import de.hybris.platform.core.threadregistry.RegistrableThread;
import de.hybris.platform.jalo.JaloSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Optional;
import java.util.concurrent.*;

/**
 * {@link ThreadPoolExecutor} implementation to ensure threads are aware of Hybris requirements
 * e.g. set with a current tenant and an active Session ready for running queries
 *
 */
@SuppressWarnings("unused")
public class HybrisAwareThreadPoolExecutor extends ThreadPoolExecutor
{
	private static final Logger LOG = LoggerFactory.getLogger(HybrisAwareThreadPoolExecutor.class);

	private final ThreadLocal<Tenant> localPreviousTenant = new ThreadLocal<>();
	private final Tenant tenant;

	public HybrisAwareThreadPoolExecutor(final Tenant tenant,
	                                     final int corePoolSize,
	                                     final int maximumPoolSize,
	                                     final long keepAliveTime,
	                                     final TimeUnit unit,
	                                     final BlockingQueue<Runnable> workQueue)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		this.tenant = tenant;
	}

	public HybrisAwareThreadPoolExecutor(final Tenant tenant,
	                                     final int corePoolSize,
	                                     final int maximumPoolSize,
	                                     final long keepAliveTime,
	                                     final TimeUnit unit,
	                                     final BlockingQueue<Runnable> workQueue,
	                                     final ThreadFactory threadFactory)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		this.tenant = tenant;
	}

	public HybrisAwareThreadPoolExecutor(final Tenant tenant,
	                                     final int corePoolSize,
	                                     final int maximumPoolSize,
	                                     final long keepAliveTime,
	                                     final TimeUnit unit,
	                                     final BlockingQueue<Runnable> workQueue,
	                                     final RejectedExecutionHandler handler)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
		this.tenant = tenant;
	}

	// Suppress Sonar warnings - Extension of a parent constructor
	@SuppressWarnings("squid:S00107")
	public HybrisAwareThreadPoolExecutor(final Tenant tenant,
	                                     final int corePoolSize,
	                                     final int maximumPoolSize,
	                                     final long keepAliveTime,
	                                     final TimeUnit unit,
	                                     final BlockingQueue<Runnable> workQueue,
	                                     final ThreadFactory threadFactory,
	                                     final RejectedExecutionHandler handler)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
		this.tenant = tenant;
	}

	@Override
	protected void beforeExecute(final Thread t, final Runnable r)
	{
		super.beforeExecute(t, r);
		prepareThread();
	}

	/**
	 * Prepare method activates a session for a passed tenant.
	 */
	@SuppressWarnings("Duplicates")
	private void prepareThread()
	{
		LOG.debug("Preparing thread {}.", Thread.currentThread().getName());

		// Store the current tenant so can reset later
		if (!Registry.isCurrentTenant(tenant))
		{
			if (Registry.hasCurrentTenant())
			{
				localPreviousTenant.set(Registry.getCurrentTenant());
			}
			else
			{
				localPreviousTenant.remove();
			}
			Registry.setCurrentTenant(tenant);
		}
		JaloSession.getCurrentSession().activate();

		// Register the thread with the suspend/resume system
		final OperationInfo operationInfo = OperationInfo.builder()
				.withCategory(OperationInfo.Category.TASK)
				.asNotSuspendableOperation()
				.build();
		RegistrableThread.registerThread(operationInfo);
	}

	@Override
	protected void afterExecute(final Runnable r, final Throwable t)
	{
		super.afterExecute(r, t);
		unPrepareThread();
	}

	/**
	 * Unprepare thread logic runs always after the pointcut proceeds
	 * <p>
	 * Default deactivates a session for a passed tenant, reverts the tenant as before a factory call.
	 */
	@SuppressWarnings("Duplicates")
	private void unPrepareThread()
	{
		// Close the session and restore any previous tenant.
		try
		{
			JaloSession.getCurrentSession().close();
		}
		finally
		{
			Optional.ofNullable(localPreviousTenant.get())
					.ifPresent(Registry::setCurrentTenant);
		}

		// Unregister the thread from the suspend/resume system
		RegistrableThread.unregisterThread();

		LOG.debug("Unprepared thread {}.", Thread.currentThread().getName());
	}

	public static ThreadPoolExecutor newSingleThreadedExecutor(final String threadPrefix)
	{
		final Tenant tenant = Registry.getCurrentTenant();
		return new HybrisAwareThreadPoolExecutor(tenant, 1, 1, 0,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
				new CustomizableThreadFactory(threadPrefix));
	}

}
