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
package de.hybris.platform.cloud.commons.aop.exception;

import de.hybris.platform.cloud.commons.services.monitor.Step;

/**
 * Exception thrown when an exception is caught during aspect monitoring of an action performed
 * as part of a {@link Step} of the cloud hot folder pipeline.
 */
public class ActionException extends Exception
{
	public ActionException(final String action, final Step step, final Exception cause)
	{
		super(String.format("Action(s): [%s] FAILED as part of Step [%s]", action, step), cause);
	}
}
