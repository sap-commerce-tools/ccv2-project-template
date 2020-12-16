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

import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipHeaderTask;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.service.UnzippedFolderImportService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @inheritDoc Delegates the header onto a {@link UnzippedFolderImportService} who's pattern mapping matches the file name
 */
public class ZipHeaderExecutionTask implements ZipHeaderTask
{
    private static final Logger LOG = getLogger(ZipHeaderExecutionTask.class);

    private Map<Pattern, UnzippedFolderImportService> dataImportServices;

    @Override
    public ZipBatchHeader executeZip(final ZipBatchHeader header)
    {
        final String zipFileName = header.getOriginalFileName();
        Assert.notNull(zipFileName, "header.originalFileName cannot be null");
        Assert.notNull(header.getFileUnzippedAs(), "header.fileUnzippedAs cannot be null");

        for (final Map.Entry<Pattern, UnzippedFolderImportService> e : getUnzippedFolderImportServices().entrySet())
        {
            final Pattern pattern = e.getKey();
            final UnzippedFolderImportService service = e.getValue();
            if (pattern.matcher(zipFileName).matches())
            {
                LOG.info("zip [{}] matches pattern [{}] for [{}], will attempt import", zipFileName, pattern, service);

                service.execute(header);
            }
            else
            {
                LOG.debug("zip [{}] didn't match pattern [{}] for [{}]", zipFileName, pattern, service);
            }
        }

        return header;
    }

    protected Map<Pattern, UnzippedFolderImportService> getUnzippedFolderImportServices()
    {
        return dataImportServices;
    }

    @Required
    public void setUnzippedFolderImportServices(final Map<Pattern, UnzippedFolderImportService> dataImportServices)
    {
        this.dataImportServices = dataImportServices;
    }

}
