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
package de.hybris.platform.cloud.azure.hotfolder.remote.file.comparators;

import com.microsoft.azure.storage.blob.CloudBlob;
import de.hybris.platform.cloud.azure.hotfolder.remote.session.AzureBlobFileInfo;
import de.hybris.platform.cloud.commons.spring.integration.file.comparators.TimestampedObject;

import java.util.Comparator;

/***
 * Adapts a {@link Comparator} of {@link TimestampedObject}s so that it can compare {@link CloudBlob} objects;
 *
 */
public class AzureBlobTimestampComparatorAdapter implements Comparator<CloudBlob>
{
    private final Comparator<TimestampedObject> comparator;

    public AzureBlobTimestampComparatorAdapter(final Comparator<TimestampedObject> comparator)
    {
        this.comparator = comparator;
    }

    public int compare(final CloudBlob o1, final CloudBlob o2)
    {
        return comparator.compare(() -> AzureBlobFileInfo.getModified(o1),
                () -> AzureBlobFileInfo.getModified(o2));
    }

}

