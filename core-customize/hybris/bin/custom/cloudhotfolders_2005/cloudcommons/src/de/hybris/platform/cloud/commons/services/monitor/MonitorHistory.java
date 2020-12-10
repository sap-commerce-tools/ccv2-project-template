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
package de.hybris.platform.cloud.commons.services.monitor;

import java.util.Date;

/**
 * Strategy to record history information for areas of the running system
 */
public interface MonitorHistory
{
	/**
	 * Record the completion of an interim action within a step
	 *
	 * @param code    code representing the unique action
	 * @param start   when started
	 * @param end     when ended
	 * @param message helpful message
	 * @param args    arguments to be substituted into the helpful message
	 * @return current instance of {@link MonitorHistory}
	 */
	MonitorHistory addAction(final String code, final Status status, final Date start, final Date end, final String message, final Object... args);

	/**
	 * Record that a given step succeeded
	 *
	 * @param step    step
	 * @param start   when started
	 * @param end     when ended
	 * @param message helpful message
	 * @param args    arguments to be substituted into the helpful message
	 * @return current instance of {@link MonitorHistory}
	 */
	MonitorHistory stepSucceeded(final Step step, final Date start, final Date end, final String message, final Object... args);

	/**
	 * Record that a given step failed
	 *
	 * @param step    step
	 * @param start   when started
	 * @param end     when ended
	 * @param ex      exception thrown
	 * @param message helpful message
	 * @param args    arguments to be substituted into the helpful message
	 * @return current instance of {@link MonitorHistory}
	 */
	MonitorHistory stepFailed(final Step step, final Date start, final Date end, final Throwable ex, final String message, final Object... args);

	/**
	 * Temporarily stop & record the history so it can be resumed at a later time
	 */
	void checkpoint();

	/**
	 * End the history and record it's information
	 *
	 * @param status what was the end state
	 */
	void end(final Status status);

}
