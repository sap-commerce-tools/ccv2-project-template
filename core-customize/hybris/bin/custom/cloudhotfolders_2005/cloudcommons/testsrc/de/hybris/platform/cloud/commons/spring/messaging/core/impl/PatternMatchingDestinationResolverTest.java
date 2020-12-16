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

import de.hybris.bootstrap.annotations.UnitTest;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.core.DestinationResolver;

import com.google.common.collect.ImmutableMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class PatternMatchingDestinationResolverTest
{
	private static final Map<Pattern, Object> PATTERN_MAP = ImmutableMap.<Pattern, Object>builder()
			.put(Pattern.compile("^ignore.*$"), "channelA")
			.put(Pattern.compile("^(\\D*)-(\\d+).txt$"), "channelB")
			.build();

	private PatternMatchingDestinationResolver<Object> resolver;

	@Mock
	private DestinationResolver<Object> fallbackResolver;

	private final Object fallbackResult = new Object();

	@Before
	public void setUp() {
	    resolver = new PatternMatchingDestinationResolver<>(fallbackResolver, PATTERN_MAP);
	}

	@Test
	public void givenNoPatternApplies_whenGetChannelKeys_thenReturnHeaderValue()
	{
		given(fallbackResolver.resolveDestination("blah.txt")).willReturn(fallbackResult);
		assertThat(resolver.resolveDestination("blah.txt")).isEqualTo(fallbackResult);
	}

	@Test
	public void givenSinglePatternApplies_whenGetChannelKeys_thenReturnValueMappedToTheMatchingPattern()
	{
		assertThat(resolver.resolveDestination("ignore.txt")).isEqualTo("channelA");
		assertThat(resolver.resolveDestination("abc-123.txt")).isEqualTo("channelB");
	}

	@Test
	public void givenMultiplePatternApplies_whenGetChannelKeys_thenReturnValueMappedToFirstMatch()
	{
		assertThat(resolver.resolveDestination("ignore-123.txt")).isEqualTo("channelA");
	}
}
