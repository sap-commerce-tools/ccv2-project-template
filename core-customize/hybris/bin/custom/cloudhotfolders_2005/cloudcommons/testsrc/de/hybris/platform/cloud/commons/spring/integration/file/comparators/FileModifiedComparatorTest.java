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
package de.hybris.platform.cloud.commons.spring.integration.file.comparators;

import de.hybris.bootstrap.annotations.UnitTest;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class FileModifiedComparatorTest
{
	private final TimestampComparator comparator = new TimestampComparator();
	private final FileTimestampComparatorAdapter adapter = new FileTimestampComparatorAdapter(comparator);

	@Test
	public void filterShouldSortFilesByModifiedTime()
	{
		final File first = mock(File.class);
		final File second = mock(File.class);
		final File third = mock(File.class);

		given(first.lastModified()).willReturn(0L);
		given(second.lastModified()).willReturn(1L);
		given(third.lastModified()).willReturn(2L);

		final List<File> entries = Arrays.asList(second, third, first);

		entries.sort(adapter);

		Assertions.assertThat(entries).containsExactly(first, second, third);
	}
}