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

/**
 * {@link Comparator} implementation that enables sorting of objects based on a timestamp
 */
public class TimestampComparator implements Comparator<TimestampedObject>
{
    @Override
    public int compare(final TimestampedObject o1, final TimestampedObject o2)
    {
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
}
