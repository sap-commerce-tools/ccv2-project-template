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
package de.hybris.platform.cloud.commons.spring.beans.factory.config.impl;

import de.hybris.platform.cloud.commons.spring.context.SmartLifecycleRole;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A {@link BeanPostProcessor} implementation to ensure all beans implementing {@link SmartLifecycleRole}
 * are registered with the {@link SmartLifecycleRoleController}
 *
 */
public class SmartLifecycleRoleBeanPostProcessor
        implements InitializingBean, SmartInitializingSingleton, BeanPostProcessor, BeanFactoryAware
{
    private static final Logger LOG = getLogger(SmartLifecycleRoleBeanPostProcessor.class);

    private static final String CONTROLLER_BEAN_NAME = IntegrationContextUtils.INTEGRATION_LIFECYCLE_ROLE_CONTROLLER;

    private final MultiValueMap<String, String> lazyLifecycleRoles = new LinkedMultiValueMap<>();

    private BeanFactory beanFactory;

    @Override
    public void afterPropertiesSet()
    {
        Assert.notNull(this.beanFactory, "BeanFactory must not be null");
    }

    @Override
    public void afterSingletonsInstantiated()
    {
        final SmartLifecycleRoleController controller
                = this.beanFactory.getBean(CONTROLLER_BEAN_NAME, SmartLifecycleRoleController.class);
        this.lazyLifecycleRoles.forEach(controller::addLifecyclesToRole);
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException
    {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException
    {
        if (bean instanceof SmartLifecycleRole)
        {
            final String role = ((SmartLifecycleRole) bean).getRole();
            LOG.debug("Processing bean with name [{}] role [{}]", beanName, role);
            lazyLifecycleRoles.add(role, beanName);
        }
        return bean;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory)
    {
        this.beanFactory = beanFactory;
    }
}
