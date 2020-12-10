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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Comparator} implementation that enables sorting of objects based on the sequence number within the object's name
 */
public class NameSequenceComparator implements Comparator<NamedObject>
{
    private static final Logger LOG = getLogger(NameSequenceComparator.class);

    private static final Long DEFAULT_PRIORITY = Long.MAX_VALUE;
    private final Pattern pattern;
    private final String sequenceGroup;

    public NameSequenceComparator(final String pattern, final String sequenceGroup)
    {
        this.pattern = Pattern.compile(pattern);
        this.sequenceGroup = sequenceGroup;
    }

    @Override
    public int compare(final NamedObject o1, final NamedObject o2)
    {
        return getSequence(o1).compareTo(getSequence(o2));
    }

    /**
     * Retrieves the sequence for a object.
     *
     * @param namedObject the object to get sequence from
     * @return the part of the object's name identified as sequence number, if one exists, otherwise the default priority
     */
    protected Long getSequence(final NamedObject namedObject)
    {
        final String name = namedObject.getName();

        return Optional.of(pattern.matcher(name))
                .map(m->getSequence(m, sequenceGroup))
                .map(NameSequenceComparator::getLong)
                .orElse(DEFAULT_PRIORITY);
    }

    protected String getSequence(final Matcher matcher, final String group)
    {
        if(matcher.matches() && matcher.groupCount() > 0)
        {
            try
            {
                return matcher.group(group);
            }
            catch(final NumberFormatException ex)
            {
                LOG.debug("Unable to find a sequence in the filename [{}].", matcher.toString());
                return null;
            }
        }
        return null;
    }

    protected static Long getLong(final String sequenceNo)
    {
        if(NumberUtils.isParsable(sequenceNo) && !StringUtils.contains(sequenceNo, "-") &&
                !StringUtils.contains(sequenceNo, "."))
        {
            try
            {
                return Long.valueOf(sequenceNo);
            }
            catch(final NumberFormatException ex)
            {
                LOG.info("Unable to cast [{}] to a long sequence, it maybe a timestamp.", sequenceNo);
                return null;
            }
        }
        return null;
    }
}
