/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.cloud.hotfolder.aop.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.UUID;

/**
 * Utilities for generating action codes.
 */
public class ActionCodeGenerator
{
	private static final int SHORT_CODE_LEN = 8;

	private ActionCodeGenerator()
	{
		//Private constructor to prevent instantiation
	}

	public static String shortAlphaNumeric()
	{
		return RandomStringUtils.randomAlphanumeric(SHORT_CODE_LEN);
	}

	public static String randomUUID()
	{
		return UUID.randomUUID().toString();
	}
}
