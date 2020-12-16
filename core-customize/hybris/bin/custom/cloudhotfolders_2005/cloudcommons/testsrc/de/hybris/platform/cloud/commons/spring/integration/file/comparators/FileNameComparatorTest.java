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
public class FileNameComparatorTest
{
	private final NameComparator comparator
			= new NameComparator("^(?<filename>.*)-(?<sequence>\\d*)(?<extension>.*)$","filename");
	private final FileNameComparatorAdatper adapter = new FileNameComparatorAdatper(comparator);

	@Test
	public void filterShouldSortFilesByName()
	{
		final File first = mock(File.class);
		final File second = mock(File.class);
		final File third = mock(File.class);

		given(first.getName()).willReturn("a-3.csv");
		given(second.getName()).willReturn("b-2.csv");
		given(third.getName()).willReturn("c-1.csv");

		final List<File> entries = Arrays.asList(second, third, first);

		entries.sort(adapter);

		Assertions.assertThat(entries).containsExactly(first, second, third);
	}
}