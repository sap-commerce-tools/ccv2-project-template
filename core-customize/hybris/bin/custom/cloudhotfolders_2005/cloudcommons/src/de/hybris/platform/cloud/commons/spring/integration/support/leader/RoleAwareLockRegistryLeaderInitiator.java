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
package de.hybris.platform.cloud.commons.spring.integration.support.leader;

import de.hybris.platform.cloud.commons.scheduling.HybrisAwareThreadPoolExecutor;
import de.hybris.platform.cloud.commons.spring.context.SmartLifecycleRole;
import de.hybris.platform.core.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.support.locks.LockRegistry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link LockRegistryLeaderInitiator} that itself has a role. This allows
 * the election to be started/stopped by a role controller.
 */
public class RoleAwareLockRegistryLeaderInitiator extends LockRegistryLeaderInitiator
		implements SmartLifecycleRole, InitializingBean
{

	private static final Logger LOG = LoggerFactory.getLogger(RoleAwareLockRegistryLeaderInitiator.class);

	private final String tenantId;
	private String role;
	private ExecutorService executorService;

	public RoleAwareLockRegistryLeaderInitiator(final LockRegistry locks,
	                                            final String childRole)
	{
		this(locks, new DefaultCandidate(
				Registry.getCurrentTenant().getTenantID() + "-" + Registry.getClusterID(), childRole));
	}

	public RoleAwareLockRegistryLeaderInitiator(final LockRegistry locks)
	{
		super(locks);
		this.tenantId = Registry.getCurrentTenant().getTenantID();
	}

	public RoleAwareLockRegistryLeaderInitiator(final LockRegistry locks, final Candidate candidate)
	{
		super(locks, candidate);
		this.tenantId = Registry.getCurrentTenant().getTenantID();
	}

	public void setRole(final String role)
	{
		this.role = role;
	}

	public String getRole()
	{
		return this.role;
	}

	@Override
	public void stop(final Runnable runnable)
	{
		super.stop(runnable);
		if (this.executorService != null && !this.executorService.isTerminated())
		{
			try
			{
				// Ensure that we don't shutdown until the executor service
				// has finished cleaning up.
				LOG.debug("Waiting for executor service termination...");
				this.executorService.awaitTermination(30, TimeUnit.SECONDS);
			}
			catch (final InterruptedException e)
			{
				Thread.currentThread().interrupt();
				//Do Nothing
			}
		}
	}

	@Override
	public void setExecutorService(final ExecutorService executorService)
	{
		// Don't allow the task executor to be passed in, we create it in afterPropertiesSet.
		throw new UnsupportedOperationException("Setting the task executor is unsupported.");
	}

	@Override
	public void afterPropertiesSet()
	{
		final String threadPrefix = "HotfolderLeaderInitiator-" + tenantId + "-";
		this.executorService = HybrisAwareThreadPoolExecutor.newSingleThreadedExecutor(threadPrefix);
		super.setExecutorService(this.executorService);
	}

	@Override
	public void destroy()
	{
		try
		{
			LOG.debug("Forcing stop.");
			stop();
		}
		finally
		{
			LOG.debug("Shutting down task executor.");
			this.executorService.shutdown();
		}
	}

}
