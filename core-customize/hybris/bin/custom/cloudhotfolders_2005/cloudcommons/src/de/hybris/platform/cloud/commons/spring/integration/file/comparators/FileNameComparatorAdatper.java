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

import java.io.File;
import java.util.Comparator;

/***
 * Adapts a {@link Comparator} of {@link NamedObject}s so that it can compare {@link File} objects;
 */
public class FileNameComparatorAdatper implements Comparator<File>
{
    private final Comparator<NamedObject> comparator;

    public FileNameComparatorAdatper(final Comparator<NamedObject> comparator)
    {
        this.comparator = comparator;
    }

    public int compare(final File o1, final File o2)
    {
        return comparator.compare(o1::getName, o2::getName);
    }

}
