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
public class FileNamePrefixComparatorTest
{
    private static final String PRODUCT = "product";
    private static final String URL_MEDIA = "url_media";

    private final NamePrefixComparator comparator = new NamePrefixComparator(PRODUCT + "," + URL_MEDIA);
    private final FileNameComparatorAdatper adapter = new FileNameComparatorAdatper(comparator);

    @Test
    public void shouldSortByNamePrefix()
    {
        final File first = mock(File.class);
        final File second = mock(File.class);

        given(first.getName()).willReturn(PRODUCT + "-999.txt");
        given(second.getName()).willReturn(URL_MEDIA + "-999.txt");

        final List<File> entries = Arrays.asList(second, first);

        entries.sort(adapter);

        Assertions.assertThat(entries).containsExactly(first, second);
    }

    @Test
    public void givenNamePrefixNotFound_thenFileShouldBeAfterNamedPrefixes()
    {
        final File first = mock(File.class);
        final File second = mock(File.class);
        final File third = mock(File.class);

        given(first.getName()).willReturn(PRODUCT + "-999.txt");
        given(second.getName()).willReturn(URL_MEDIA + "-999.txt");
        given(third.getName()).willReturn("another-999.txt");

        final List<File> entries = Arrays.asList(second, third, first);

        entries.sort(adapter);

        Assertions.assertThat(entries).containsExactly(first, second, third);
    }

}