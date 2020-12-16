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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.task;

import de.hybris.platform.acceleratorservices.dataimport.batch.BatchHeader;
import de.hybris.platform.acceleratorservices.dataimport.batch.task.CleanupTask;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipHeaderTask;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @inheritDoc Also cleans up the parent folder of the Unzipped File
 */
public class ZipHeaderCleanUpTask extends CleanupTask implements ZipHeaderTask
{
    private static final Logger LOG = getLogger(ZipHeaderCleanUpTask.class);

    /**
     * Default implementation with {@link BatchHeader} to ensure that AOP aspects are applied
     *
     * @param header to be processed
     * @throws IllegalArgumentException when header is not of type {@link ZipBatchHeader}
     */
    @Override
    public BatchHeader execute(final BatchHeader header)
    {
        Assert.isInstanceOf(ZipBatchHeader.class, header);
        return executeZip((ZipBatchHeader) header);
    }

    @Override
    public ZipBatchHeader executeZip(final ZipBatchHeader header)
    {
        super.execute(header);
        Optional.ofNullable(header.getUnzippedFolder())
                .ifPresent(this::deleteDirectory);
        return null;
    }

    protected void deleteDirectory(final File dir)
    {
        LOG.debug("Going to delete directory [{}]", dir);
        FileUtils.deleteQuietly(dir);
    }
}
