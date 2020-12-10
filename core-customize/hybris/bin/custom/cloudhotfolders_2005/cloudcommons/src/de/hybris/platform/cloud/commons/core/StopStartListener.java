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
package de.hybris.platform.cloud.commons.core;

import de.hybris.platform.cloud.commons.suspend.SuspendResumeListener;
import de.hybris.platform.core.TenantListener;

/**
 * An extension of {@link TenantListener} and {@link SuspendResumeListener} that allows direct start/stop calls
 *
 */
public interface StopStartListener extends TenantListener, SuspendResumeListener
{
	void start();

	void stop();
}
