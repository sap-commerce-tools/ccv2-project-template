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
package de.hybris.platform.cloud.commons.spring.messaging.core.impl;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * {@link DestinationResolver} implementation that attempts to locate a destination by
 * checking if any pattern matches the required name, otherwise it delegates onto another
 * {@link DestinationResolver} implementation as a fallback mechanism
 *
 * @see DestinationResolver#resolveDestination
 */
public class PatternMatchingDestinationResolver<D> implements DestinationResolver<D>
{
	private final DestinationResolver<D> fallbackResolver;
	private final Map<Pattern, D> patternChannelMappings;

	/**
	 * Create a new PatternMatchingResolver using the given pattern->channel mappings and
	 * fallback DestinationResolver to resolve destinations in the event of no match.
	 *
	 * @param fallbackResolver       the fallback DestinationResolver to delegate to when no match found
	 * @param patternChannelMappings map of {@link Pattern} to destinations
	 */
	public PatternMatchingDestinationResolver(final DestinationResolver<D> fallbackResolver, final Map<Pattern, D> patternChannelMappings)
	{
		Assert.notNull(fallbackResolver, "'fallbackResolver' cannot be null");
		Assert.notNull(patternChannelMappings, "'patternChannelMappings' cannot be null");
		this.fallbackResolver = fallbackResolver;
		this.patternChannelMappings = patternChannelMappings;
	}

	/**
	 * Checks patterns, and uses the first match, otherwise delegates onto <code>fallbackResolver</code>
	 * @param name the destination name to be resolved
	 * @return the matched destination or that returned by <code>fallbackResolver</code>
	 * @throws DestinationResolutionException if the target DestinationResolver
	 * reports an error during destination resolution
	 */
	@Override
	public D resolveDestination(final String name) throws DestinationResolutionException
	{
		return patternChannelMappings.entrySet().stream()
				.filter(e -> e.getKey().matcher(name).matches())
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseGet(() -> fallbackResolver.resolveDestination(name));
	}

}
