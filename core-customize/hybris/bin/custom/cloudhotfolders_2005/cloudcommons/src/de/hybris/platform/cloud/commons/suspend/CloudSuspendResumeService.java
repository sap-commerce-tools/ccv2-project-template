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
import de.hybris.platform.core.suspend.ResumeTokenVerificationFailed;
import de.hybris.platform.core.suspend.SuspendOptions;
import de.hybris.platform.core.suspend.SuspendResult;
import de.hybris.platform.core.threadregistry.DefaultSuspendResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Extends {@link DefaultSuspendResumeService}, adding the ability to notify registered {@link SuspendResumeListener}
 * instances of suspend/resume events.
 */
public class CloudSuspendResumeService extends DefaultSuspendResumeService
{

	private static final Logger LOG = LoggerFactory.getLogger(CloudSuspendResumeService.class);

	@Override
	public SuspendResult suspend(final SuspendOptions suspendOptions)
	{
		beforeSuspend(suspendOptions);
		final SuspendResult suspendResult = super.suspend(suspendOptions);
		afterSuspend(suspendOptions, suspendResult);
		return suspendResult;
	}

	@Override
	public void resume(final ResumeOptions resumeOptions) throws ResumeTokenVerificationFailed
	{
		beforeResume(resumeOptions);
		super.resume(resumeOptions);
		afterResume(resumeOptions);
	}

	protected void beforeSuspend(final SuspendOptions suspendOptions)
	{
		for (final SuspendResumeListener listener : getSuspendResumeListeners())
		{
			try
			{
				listener.beforeSuspend(suspendOptions);
			}
			catch (final Exception t)
			{
				LOG.error("Error calling suspend resume listener [{}] before suspend.", listener, t);
			}
		}
	}

	protected void afterSuspend(final SuspendOptions suspendOptions, final SuspendResult suspendResult)
	{
		for (final SuspendResumeListener listener : getSuspendResumeListeners())
		{
			try
			{
				listener.afterSuspend(suspendOptions, suspendResult);
			}
			catch (final Exception t)
			{
				LOG.error("Error calling suspend resume listener [{}] after suspend.", listener, t);
			}
		}
	}

	protected void beforeResume(final ResumeOptions resumeOptions)
	{
		for (final SuspendResumeListener listener : getSuspendResumeListeners())
		{
			try
			{
				listener.beforeResume(resumeOptions);
			}
			catch (final Exception t)
			{
				LOG.error("Error calling suspend resume listener [{}] before resume.", listener, t);
			}
		}
	}

	protected void afterResume(final ResumeOptions resumeOptions)
	{
		for (final SuspendResumeListener listener : getSuspendResumeListeners())
		{
			try
			{
				listener.afterResume(resumeOptions);
			}
			catch (final Exception t)
			{
				LOG.error("Error calling suspend resume listener [{}] after resume.", listener, t);
			}
		}
	}

	protected List<SuspendResumeListener> getSuspendResumeListeners()
	{
		return CloudSuspendResumeRegistry.getSuspendResumeListeners();
	}

}
