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

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * {@link Comparator} implementation that enables sorting of objects based on the object's name
 */
public class NameComparator implements Comparator<NamedObject>
{
    private final Pattern pattern;
    private final String nameGroup;

    public NameComparator(final String pattern, final String nameGroup)
    {
        this.pattern = Pattern.compile(pattern);
        this.nameGroup = nameGroup;
    }

    @Override
    public int compare(final NamedObject o1, final NamedObject o2)
    {
        return getName(o1).compareTo(getName(o2));
    }

    /**
     * Retrieves the name for an object, excluding sequence numbers.
     *
     * @param namedObject the object to get sequence from
     * @return the part of the object's name excluding sequence number, if one exists, otherwise the full name
     */
    protected String getName(final NamedObject namedObject)
    {
        final String name = namedObject.getName();

        return Optional.of(pattern.matcher(name))
                .filter(m -> m.matches() && m.groupCount() > 0)
                .map(m -> m.group(nameGroup))
                .orElse("");
    }
}
