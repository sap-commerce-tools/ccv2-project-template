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
package de.hybris.platform.cloud.commons.jalo;

import de.hybris.platform.cloud.commons.constants.CloudCommonsConstants;
import de.hybris.platform.cloud.commons.core.StopStartListener;
import de.hybris.platform.cloud.commons.core.impl.HybrisGroupRoleController;
import de.hybris.platform.core.Registry;
import de.hybris.platform.util.JspContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * This is the extension manager of the CloudCommons extension.
 */
public class CloudCommonsManager extends GeneratedCloudCommonsManager
{
	private static final Logger LOG = getLogger(CloudCommonsManager.class);

	/**
	 * Get the valid instance of this manager.
	 * @return the current instance of this manager
	 */
	public static CloudCommonsManager getInstance()
	{
		return (CloudCommonsManager) Registry.getCurrentTenant().getJaloConnection().getExtensionManager()
				.getExtension(CloudCommonsConstants.EXTENSIONNAME);
	}

	@Override
	public void notifyInitializationStart(@SuppressWarnings("unused") final Map<String, String> params,
	                                      @SuppressWarnings("unused") final JspContext ctx) throws Exception // NOPMD
	{
		notifyTenantListener(HybrisGroupRoleController.BEAN_ID, StopStartListener::stop);
	}

	@Override
	public void notifyInitializationEnd(@SuppressWarnings("unused") final Map params,
	                                    @SuppressWarnings("unused") final JspContext ctx) throws Exception //NOPMD
	{
		notifyTenantListener(HybrisGroupRoleController.BEAN_ID, StopStartListener::start);
	}

	protected void notifyTenantListener(final String beanId, final Consumer<StopStartListener> notifyMethod)
	{
		final ApplicationContext applicationContext = Registry.getCoreApplicationContext();

		try
		{
			Optional.of(applicationContext.getBean(beanId, StopStartListener.class))
					.ifPresent(notifyMethod);
		}
		catch (final BeansException e)
		{
			LOG.error("There was a problem getting the bean [{}] so cannot notify the listener", beanId, e);
		}
	}
}
