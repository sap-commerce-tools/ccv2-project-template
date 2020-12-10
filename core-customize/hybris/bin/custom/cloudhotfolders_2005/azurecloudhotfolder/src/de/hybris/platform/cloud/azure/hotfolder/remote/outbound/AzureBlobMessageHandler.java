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
package de.hybris.platform.cloud.azure.hotfolder.remote.outbound;

import de.hybris.platform.cloud.azure.hotfolder.remote.AzureBlobEntryFileTemplate;
import de.hybris.platform.cloud.azure.hotfolder.remote.file.RemoteFileHeaders;
import de.hybris.platform.cloud.azure.hotfolder.remote.session.AzureBlobSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.SessionFactory;

import com.microsoft.azure.storage.blob.CloudBlob;

import org.springframework.messaging.Message;

/**
 * An Azure Blob Storage specific {@link FileTransferringMessageHandler} extension.
 * Based on the {@link AzureBlobSession} and {@link AzureBlobEntryFileTemplate}.
 *
 * @author Tom Greasley (tgreasley@salmon.com)
 * @see AzureBlobEntryFileTemplate
 * @see AzureBlobSession
 */
public class AzureBlobMessageHandler extends FileTransferringMessageHandler<CloudBlob>
{
    private static final Logger LOG = LoggerFactory.getLogger(AzureBlobMessageHandler.class);

    private volatile boolean deleteSourceFiles;

    public AzureBlobMessageHandler(final SessionFactory<CloudBlob> sessionFactory)
    {
        this(new AzureBlobEntryFileTemplate(sessionFactory));
    }

    public AzureBlobMessageHandler(final AzureBlobEntryFileTemplate remoteFileTemplate)
    {
        super(remoteFileTemplate);
    }

    public AzureBlobMessageHandler(final RemoteFileTemplate<CloudBlob> remoteFileTemplate)
    {
        super(remoteFileTemplate);
    }

    /**
     * Specify whether to delete source Files after writing to the destination
     * directory. The default is <em>false</em>. When set to <em>true</em>, it
     * will only have an effect if the inbound Message has a File payload or
     * a {@link RemoteFileHeaders#ORIGINAL_FILE} header value containing either a
     * File instance or a String representing the original file path.
     *
     * @param deleteSourceFiles true to delete the source files.
     */
    public void setDeleteSourceFiles(final boolean deleteSourceFiles)
    {
        this.deleteSourceFiles = deleteSourceFiles;
    }

    public void setRemoteDirectory(final String remoteDirectory)
    {
        this.setRemoteDirectoryExpression(EXPRESSION_PARSER.parseExpression("'" + remoteDirectory + "'"));
    }

    @Override
    protected void handleMessageInternal(final Message<?> message)
    {
        final File file = retrieveFile(message);
        if (file == null || !file.exists())
        {
            LOG.info("File not found for message [{}]", message);
        }
        else
        {
            super.handleMessageInternal(message);
            if (deleteSourceFiles)
            {
                cleanUpAfterCopy(file);
            }
        }
    }

    protected void cleanUpAfterCopy(final File file)
    {
        if (file != null && file.exists())
        {
            try
            {
                Files.delete(file.toPath());
            }
            catch (final IOException ex)
            {
                LOG.info("Cannot delete local file '" + file +
                        "'. The local file may be busy in some other process.", ex);
            }
        }
    }

    private File retrieveFile(final Message<?> message)
    {
        final Object payload = message.getPayload();
        if (payload instanceof File)
        {
            return (File) payload;
        }
        else
        {
            return retrieveOriginalFileFromHeader(message);
        }
    }

    /**
     * Retrieves the File instance from the {@link RemoteFileHeaders#ORIGINAL_FILE}
     * header if available. If the value is not a File instance or a String
     * representation of a file path, this will return <code>null</code>.
     */
    private File retrieveOriginalFileFromHeader(final Message<?> message)
    {
        final Object value = message.getHeaders().get(RemoteFileHeaders.ORIGINAL_FILE);
        if (value instanceof File)
        {
            return (File) value;
        }
        if (value instanceof String)
        {
            return new File((String) value);
        }
        return null;
    }

}
