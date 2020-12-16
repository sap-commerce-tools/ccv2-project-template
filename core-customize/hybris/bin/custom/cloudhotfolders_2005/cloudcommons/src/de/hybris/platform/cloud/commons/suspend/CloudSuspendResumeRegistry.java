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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for {@link SuspendResumeListener} instances.
 */
@SuppressWarnings("unused")
public class CloudSuspendResumeRegistry
{
	private static final Logger LOG = LoggerFactory.getLogger(CloudSuspendResumeRegistry.class);

	private static final List<SuspendResumeListener> suspendResumeListeners
			= new CopyOnWriteArrayList<>();

	private static final List<SuspendResumeListener> publicSuspendResumeListeners
			= Collections.unmodifiableList(suspendResumeListeners);

	private CloudSuspendResumeRegistry()
	{
		//NOOP
	}

	/**
	 * Registers a {@link SuspendResumeListener} which will be then notified on suspend/resume.
	 *
	 * @param listener
	 *           the listener to register for notifications
	 */
	public static void registerSuspendResumeListener(final SuspendResumeListener listener)
	{
		if( suspendResumeListeners.contains(listener))
		{
			LOG.error("Suspend Resume listener "+listener+" already registered!");
		}
		else
		{
			suspendResumeListeners.add(listener);
		}
	}

	/**
	 * Unregisters a {@link SuspendResumeListener} for not be notified on suspend/resume anymore.
	 *
	 * @param listener
	 *           the listener to unregister for notifications
	 */
	public static void unregisterSuspendResumeListener(final SuspendResumeListener listener)
	{
		suspendResumeListeners.remove(listener);
	}

	/**
	 * Gets all listeners registered for getting suspend/resume notifications.
	 *
	 * @return Unmodifiable list of all registered listeners
	 * @since 3.1-u1
	 */
	public static List<SuspendResumeListener> getSuspendResumeListeners()
	{
		return publicSuspendResumeListeners;
	}

}
