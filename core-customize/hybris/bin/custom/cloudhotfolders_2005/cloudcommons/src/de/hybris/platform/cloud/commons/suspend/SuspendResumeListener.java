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
package de.hybris.platform.cloud.commons.suspend;

import de.hybris.platform.core.suspend.ResumeOptions;
import de.hybris.platform.core.suspend.SuspendOptions;
import de.hybris.platform.core.suspend.SuspendResult;

/**
 * Implementations of this interface are notified of system suspend/resume events.  To receive events,
 * register with {@link CloudSuspendResumeRegistry#registerSuspendResumeListener(SuspendResumeListener)}.
 */
public interface SuspendResumeListener
{

	/**
	 * Called before system suspend.  The suspend operation will block on this method.
	 *
	 * @param suspendOptions - the suspend options.
	 */
	void beforeSuspend(SuspendOptions suspendOptions);

	/**
	 * Called after system suspension.
	 *
	 * @param suspendOptions - the suspend options.
	 * @param suspendResult  - the result of the system suspension.
	 */
	void afterSuspend(SuspendOptions suspendOptions, SuspendResult suspendResult);

	/**
	 * Called before system resume.  The resume operation will block on this method.
	 *
	 * @param resumeOptions - the resume options.
	 */
	void beforeResume(ResumeOptions resumeOptions);

	/**
	 * Called after system resume.
	 *
	 * @param resumeOptions - the resume options.
	 */
	void afterResume(ResumeOptions resumeOptions);

}
