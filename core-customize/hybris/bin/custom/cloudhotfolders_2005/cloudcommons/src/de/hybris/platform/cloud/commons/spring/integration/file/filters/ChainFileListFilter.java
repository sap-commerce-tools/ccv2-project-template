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
package de.hybris.platform.cloud.commons.spring.integration.file.filters;

import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.util.Assert;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A copy of {@link org.springframework.integration.file.filters.ChainFileListFilter} with a public
 * constructor that accepts a list of file filters.
 *
 * @param <F> The type that will be filtered.
 */
public class ChainFileListFilter<F> extends CompositeFileListFilter<F>
{

    public ChainFileListFilter()
    {
        super();
    }

    public ChainFileListFilter(final Collection<? extends FileListFilter<F>> fileFilters)
    {
        super(fileFilters);
    }

    @Override
    public List<F> filterFiles(final F[] files)
    {
        Assert.notNull(files, "'files' should not be null");
        List<F> leftOver = Arrays.asList(files);
        for (final FileListFilter<F> fileFilter : this.fileFilters)
        {
            if (leftOver.isEmpty())
            {
                break;
            }
            @SuppressWarnings("unchecked") final F[] fileArray =
                    leftOver.toArray((F[]) Array.newInstance(leftOver.get(0).getClass(), leftOver.size()));
            leftOver = fileFilter.filterFiles(fileArray);
        }
        return leftOver;
    }

}