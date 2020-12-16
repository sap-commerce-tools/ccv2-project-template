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
/*
 * [y] hybris Platform
 *
 * Copyright (c) 2017 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.cloud.commons.spring.util;

import com.google.common.collect.Lists;
import de.hybris.platform.testframework.runlistener.CustomRunListener;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class NeedsRunningSpringServicesTestRunListener extends CustomRunListener implements BeanFactoryAware, InitializingBean
{
	private static final Logger LOG = LoggerFactory.getLogger(NeedsRunningSpringServicesTestRunListener.class);

	private BeanFactory beanFactory;
	private SmartLifecycleRoleController controller;
	private String controllerBeanId;

	private Description description;
	private List<String> roles;

	@Override
	public void afterPropertiesSet()
	{
		Assert.notNull(this.beanFactory, "beanFactory cannot be null");
		this.controller = this.beanFactory.getBean(controllerBeanId, SmartLifecycleRoleController.class);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void testRunStarted(final Description description)
	{
		this.description = description;
		this.roles = needsRunningServiceRoles(description);

		if (CollectionUtils.isNotEmpty(roles))
		{
			LOG.warn("Starting Spring Integration Lifecycles for roles " + this.roles + " before the test: " + this.description);
			roles.forEach(controller::startLifecyclesInRole);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void testRunFinished(final Result result)
	{
		if (CollectionUtils.isNotEmpty(roles))
		{
			LOG.warn("Stopping Spring Integration Lifecycles for roles " + this.roles + " after the test: " + this.description);
			Lists.reverse(this.roles).forEach(controller::stopLifecyclesInRole);
		}
	}

	private List<String> needsRunningServiceRoles(final Description description)
	{
		return Optional.ofNullable(description.getAnnotation(NeedsRunningSpringServices.class))
				.map(NeedsRunningSpringServices::roles)
				.map(Arrays::asList)
				.orElse(Collections.emptyList());
	}

	@Required
	public void setControllerBeanId(final String controllerBeanId)
	{
		this.controllerBeanId = controllerBeanId;
	}

	@Override
	public void setBeanFactory(final BeanFactory beanFactory) throws BeansException
	{
		this.beanFactory = beanFactory;
	}
}
