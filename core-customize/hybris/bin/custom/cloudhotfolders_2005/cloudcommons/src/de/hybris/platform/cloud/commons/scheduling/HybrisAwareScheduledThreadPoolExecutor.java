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

import java.util.Optional;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * {@link ScheduledThreadPoolExecutor} implementation to ensure threads are aware of Hybris requirements
 * e.g. set with a current tenant and an active Session ready for running queries
 *
 */
@SuppressWarnings("unused")
public class HybrisAwareScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor
{
	private static final Logger LOG = LoggerFactory.getLogger(HybrisAwareScheduledThreadPoolExecutor.class);

	private final ThreadLocal<Tenant> localPreviousTenant = new ThreadLocal<>();
	private final Tenant tenant;

	public HybrisAwareScheduledThreadPoolExecutor(final Tenant tenant, final int corePoolSize)
	{
		super(corePoolSize);
		this.tenant = tenant;
	}

	public HybrisAwareScheduledThreadPoolExecutor(final Tenant tenant, final int corePoolSize, final ThreadFactory threadFactory)
	{
		super(corePoolSize, threadFactory);
		this.tenant = tenant;
	}

	public HybrisAwareScheduledThreadPoolExecutor(final Tenant tenant, final int corePoolSize, final RejectedExecutionHandler handler)
	{
		super(corePoolSize, handler);
		this.tenant = tenant;
	}

	public HybrisAwareScheduledThreadPoolExecutor(final Tenant tenant,
	                                              final int corePoolSize,
	                                              final ThreadFactory threadFactory,
	                                              final RejectedExecutionHandler handler)
	{
		super(corePoolSize, threadFactory, handler);
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
		if(!Registry.isCurrentTenant(tenant))
		{
			if(Registry.hasCurrentTenant())
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
	 * Un-prepare thread logic runs always after the pointcut proceeds
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
}
