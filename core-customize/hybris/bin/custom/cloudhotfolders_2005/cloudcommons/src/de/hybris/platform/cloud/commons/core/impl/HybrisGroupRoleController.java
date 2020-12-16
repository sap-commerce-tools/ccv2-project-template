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
package de.hybris.platform.cloud.commons.core.impl;

import com.google.common.collect.Lists;
import de.hybris.platform.cloud.commons.core.StopStartListener;
import de.hybris.platform.cloud.commons.suspend.CloudSuspendResumeRegistry;
import de.hybris.platform.cloud.commons.suspend.SuspendResumeListener;
import de.hybris.platform.core.Initialization;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.Tenant;
import de.hybris.platform.core.TenantListener;
import de.hybris.platform.core.suspend.ResumeOptions;
import de.hybris.platform.core.suspend.SuspendOptions;
import de.hybris.platform.core.suspend.SuspendResult;
import de.hybris.platform.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.util.Assert;

import java.util.List;

/**
 * A {@link TenantListener}, {@link SuspendResumeListener} and {@link StopStartListener} that calls start/stop on a spring
 * integration {@link SmartLifecycleRoleController}, ensuring that hybris is ready to process spring integration messages.
 *
 */
public class HybrisGroupRoleController implements StopStartListener, InitializingBean
{
	public static final String BEAN_ID = "hybrisGroupRoleController";

	private static final Logger LOG = LoggerFactory.getLogger(HybrisGroupRoleController.class);

	private final SmartLifecycleRoleController controller;

	private Tenant tenant;

	public HybrisGroupRoleController(final SmartLifecycleRoleController controller)
	{
		Assert.notNull(controller, "Controller must not be null.");
		this.controller = controller;
	}

	@Override
	public void afterPropertiesSet()
	{
		Registry.registerTenantListener(this);
		CloudSuspendResumeRegistry.registerSuspendResumeListener(this);
		this.tenant = Registry.getCurrentTenant();
	}

	@Override
	public void beforeSuspend(final SuspendOptions suspendOptions)
	{
		if (this.tenant != null)
		{
			stopAutoStartRoles();
		}
	}

	@Override
	public void afterSuspend(final SuspendOptions suspendOptions, final SuspendResult suspendResult)
	{
		// Do Nothing.
	}

	@Override
	public void beforeResume(final ResumeOptions resumeOptions)
	{
		// Do Nothing
	}

	@Override
	public void afterResume(final ResumeOptions resumeOptions)
	{
		if (this.tenant != null && isTenantInitialized(this.tenant) && !isTenantInitializing(this.tenant))
		{
			startAutoStartRoles();
		}

	}

	@Override
	public void beforeTenantShutDown(final Tenant tenant)
	{
		if (this.tenant == tenant)
		{
			stopAutoStartRoles();
		}
	}

	@Override
	public void afterTenantStartUp(final Tenant tenant)
	{
		if (this.tenant == tenant && isTenantInitialized(tenant) && !isTenantInitializing(tenant))
		{
			startAutoStartRoles();
		}
	}

	@Override
	public void afterSetActivateSession(final Tenant tenant)
	{
		// Do Nothing
	}

	@Override
	public void beforeUnsetActivateSession(final Tenant tenant)
	{
		// Do Nothing
	}

	@Override
	public void start()
	{
		startAutoStartRoles();
	}

	@Override
	public void stop()
	{
		stopAutoStartRoles();
	}

	protected void startAutoStartRoles()
	{
		LOG.debug("Starting auto start roles for tenant [{}].", tenant.getTenantID());
		for (final String group : getClusterGroups())
		{
			try
			{
				controller.startLifecyclesInRole(group);
			}
			catch (final RuntimeException ex)
			{
				LOG.error("Error whilst starting services in role [{}] for tenant [{}].",
						group, tenant.getTenantID(), ex);
			}
		}
	}

	protected void stopAutoStartRoles()
	{
		LOG.debug("Stopping auto start roles for tenant [{}].", tenant.getTenantID());
		for (final String group : Lists.reverse(getClusterGroups()))
		{
			try
			{
				controller.stopLifecyclesInRole(group);
			}
			catch (final RuntimeException ex)
			{
				LOG.error("Error whilst stopping services in role [{}] for tenant [{}].",
						group, tenant.getTenantID(), ex);
			}
		}
	}

	protected List<String> getClusterGroups()
	{
		return Lists.newArrayList(Registry.getClusterGroups());
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	protected boolean isTenantInitializing(final Tenant tenant)
	{

		Tenant prev = null;
		if(Registry.hasCurrentTenant())
		{
			prev = Registry.getCurrentTenant();
		}

		try
		{
			//Initialization.isTenantInitializing requires a current tenant...
			Registry.setCurrentTenant(tenant);
			final boolean tenantInitializing = Initialization.isTenantInitializing(tenant);
			LOG.debug("Tenant [{}] is initializing: {}", tenant.getTenantID(), tenantInitializing);
			return tenantInitializing;
		}
		finally
		{
			if(prev == null)
			{
				Registry.unsetCurrentTenant();
			}
			else
			{
				Registry.setCurrentTenant(prev);
			}
		}
	}

	protected boolean isTenantInitialized(final Tenant tenant)
	{
		final boolean isTenantInitialized = Utilities.isSystemInitialized(tenant.getDataSource());
		LOG.debug("Tenant [{}] is initialized: {}", tenant.getTenantID(), isTenantInitialized);
		return isTenantInitialized;
	}

}
