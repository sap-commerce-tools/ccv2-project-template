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

import com.google.common.base.Splitter;

import java.util.Comparator;
import java.util.List;

/**
 * {@link Comparator} implementation that enables sorting of objects based on a name prefix
 */
public class NamePrefixComparator implements Comparator<NamedObject>
{
    private static final Splitter SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
    private static final Integer DEFAULT_PRIORITY = Integer.MAX_VALUE;

    private final List<String> prefixPriority;

    public NamePrefixComparator(final String namePrefixPriority)
    {
        prefixPriority = SPLITTER.splitToList(namePrefixPriority);
    }

    @Override
    public int compare(final NamedObject o1, final NamedObject o2)
    {
        return getPriority(o1).compareTo(getPriority(o2));
    }

    /**
     * Retrieves the priority for a named object.
     *
     * @param namedObject the object to get priority for
     * @return the configured priority, if one exists, otherwise the default priority
     */
    protected Integer getPriority(final NamedObject namedObject)
    {
        final String name = namedObject.getName();
        return this.prefixPriority.stream()
                .filter(name::startsWith)
                .map(this.prefixPriority::indexOf)
                .findFirst()
                .orElse(DEFAULT_PRIORITY);
    }

}
