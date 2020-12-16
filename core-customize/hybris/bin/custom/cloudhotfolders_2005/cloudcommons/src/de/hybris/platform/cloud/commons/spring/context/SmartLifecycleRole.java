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
package de.hybris.platform.cloud.commons.spring.context;

import org.springframework.context.SmartLifecycle;

/**
 * A {@link SmartLifecycle} that has a role.
 *
 */
public interface SmartLifecycleRole extends SmartLifecycle
{
	String getRole();
}
