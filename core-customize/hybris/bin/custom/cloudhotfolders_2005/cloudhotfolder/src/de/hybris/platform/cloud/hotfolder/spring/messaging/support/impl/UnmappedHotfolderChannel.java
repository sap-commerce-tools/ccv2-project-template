/*
 * [y] hybris Platform
 *
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.cloud.hotfolder.spring.messaging.support.impl;

import de.hybris.platform.cloud.commons.services.monitor.MonitorService;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.channel.NullChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.slf4j.LoggerFactory.getLogger;

public class UnmappedHotfolderChannel extends NullChannel
{
    private static final Logger LOG = getLogger(UnmappedHotfolderChannel.class);

    private String fileNameHeaderKey;
    private String fileLastModifiedHeaderKey;
    private MonitorService monitorService;

    @Override
    public boolean send(final Message<?> message) {

        if(message != null)
        {
            recordHistory(message);
            if (message.getPayload() instanceof File)
            {
                final File file = (File) message.getPayload();
                deleteFile(file);
            }
        }
        return super.send(message);
    }

    protected void deleteFile(final File file)
    {
        if (file != null && file.isFile() && file.exists())
        {
            try
            {
                LOG.debug("Cleaning up [{}].", file.getAbsolutePath());
                Files.delete(file.toPath());
            }
            catch (final IOException e)
            {
                LOG.error("Unable to clean up local file [{}].", file.getAbsolutePath(), e);
            }
        }
    }

    protected void recordHistory(final Message<?> message)
    {
        final MessageHeaders headers = message.getHeaders();
        final String fileName = (String) headers.get(getFileNameHeaderKey());
        final long modified = (long) headers.get(getFileLastModifiedHeaderKey());
        getMonitorService().resume(SystemArea.HOT_FOLDER, fileName + modified)
                .stepFailed(Step.FILE_ROUTED, null, null, null, "File [{}] modified [{}] was not routed as didn't match any configurations", fileName, modified)
                .end(Status.WARNING);
    }

    protected String getFileNameHeaderKey()
    {
        return fileNameHeaderKey;
    }

    @Required
    public void setFileNameHeaderKey(final String fileNameHeaderKey)
    {
        this.fileNameHeaderKey = fileNameHeaderKey;
    }

    protected String getFileLastModifiedHeaderKey()
    {
        return fileLastModifiedHeaderKey;
    }

    @Required
    public void setFileLastModifiedHeaderKey(final String fileLastModifiedHeaderKey)
    {
        this.fileLastModifiedHeaderKey = fileLastModifiedHeaderKey;
    }

    protected MonitorService getMonitorService()
    {
        return monitorService;
    }

    @Required
    public void setMonitorService(final MonitorService monitorService)
    {
        this.monitorService = monitorService;
    }

}
