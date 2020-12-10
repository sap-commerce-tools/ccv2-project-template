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
package de.hybris.platform.cloud.commons.utils;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A utility class that makes it easier to work with enums.
 */
public final class EnumHelper
{
	private static final Logger LOG = getLogger(EnumHelper.class);

	private EnumHelper()
	{
	}

	/**
	 * Get enum value representing the string value a given object
	 * @param enumClass the enum class required
	 * @param value the value to attempt to find
	 * @return Enum value representing the given object
	 * @throws IllegalArgumentException when <code>value</code> is not a valid option
	 */
	public static <E extends Enum<E>> E getEnum(final Class<E> enumClass, final Object value)
	{
		return Optional.of(EnumUtils.getEnum(enumClass, String.valueOf(value)))
				.orElseThrow(() -> new IllegalArgumentException(value + " was not a valid value of " + enumClass));
	}

	/**
	 * Get enum value representing the string value a given object
	 * @param enumClass the enum class required
	 * @param value the value to attempt to find
	 * @return Enum value representing the given object, or else the given default value
	 */
	public static <E extends Enum<E>> E getEnum(final Class<E> enumClass, final Object value, final E defaultValue)
	{
		return Optional.of(EnumUtils.getEnum(enumClass, String.valueOf(value)))
				.orElseGet(() -> {
					LOG.warn("enum [{}] did not contain value [{}] will use default of [{}]", enumClass, value, defaultValue);
					return defaultValue;
				});
	}
}
