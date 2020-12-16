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
 * Exception thrown when an exception is caught during aspect monitoring of a {@link Step} of the cloud hot folder pipeline.
 */
public class StepException extends Exception
{
	public StepException(final Step step, final Exception cause)
	{
		super(String.format("Step: [%s] FAILED", step), cause);
	}
}
