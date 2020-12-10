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
package de.hybris.platform.cloud.azure.hotfolder.remote.inbound;

import de.hybris.platform.cloud.azure.hotfolder.remote.file.RemoteFileHeaders;
import de.hybris.platform.cloud.azure.hotfolder.remote.session.AzureBlobFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation for Azure Blob Storage.
 *
 */
public class AzureBlobSynchronizingMessageSource extends AbstractMessageSource<File> implements Lifecycle
{
	private static final Logger LOG = LoggerFactory.getLogger(AzureBlobSynchronizingMessageSource.class);

	private volatile int maxFetchSize = Integer.MIN_VALUE;

	/**
	 * An implementation that will handle the chores of actually connecting to and synchronizing
	 * the remote file system with the local one, in an inbound direction.
	 */
	private final AzureBlobInboundSynchronizer synchronizer;

	private final BlockingQueue<AzureBlobFileInfo> toBeReceived = new LinkedBlockingQueue<>();

    /**
     * Should the endpoint attempt to create the local directory? True by default.
     */
    private boolean autoCreateLocalDirectory = true;

    /**
     * Directory to which things should be synchronized locally.
     */
    private File localDirectory;

	private final AtomicBoolean running = new AtomicBoolean();

	public AzureBlobSynchronizingMessageSource(final AzureBlobInboundSynchronizer synchronizer)
	{
		Assert.notNull(synchronizer, "synchronizer must not be null");
		this.synchronizer = synchronizer;
	}

	public void setMaxFetchSize(final int maxFetchSize)
    {
        this.maxFetchSize = maxFetchSize;
    }

    public int getMaxFetchSize()
    {
        return this.maxFetchSize;
    }

    public void setAutoCreateLocalDirectory(final boolean autoCreateLocalDirectory)
    {
        this.autoCreateLocalDirectory = autoCreateLocalDirectory;
    }

	public void setLocalDirectory(final File localDirectory)
    {
        this.localDirectory = localDirectory;
        try
        {
            LOG.debug("Using local directory [{}].", localDirectory.getCanonicalPath());
        }
        catch (final IOException e)
        {
            LOG.debug("Unable to get path for local directory.", e);
        }
    }

	@Override
	public String getComponentType()
	{
		return "azure:inbound-channel-adapter";
	}

	@Override
    public void onInit()
    {
	    Assert.notNull(this.localDirectory, "localDirectory must not be null");
	    try
	    {
		    if (!this.localDirectory.exists())
		    {
			    if (this.autoCreateLocalDirectory)
			    {
			    	LOG.debug("The '" + this.localDirectory + "' directory doesn't exist; Will create.");
				    //noinspection ResultOfMethodCallIgnored
				    this.localDirectory.mkdirs(); //NOSONAR - result ignored.
			    }
			    else
			    {
				    throw new FileNotFoundException(this.localDirectory.getName());
			    }
		    }
	    }
        catch (final RuntimeException e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            throw new BeanInitializationException("Failure during initialization for: " + this, e);
        }
    }

	@Override
	public void start()
	{
		this.running.set(true);
	}

	@Override
	public void stop()
	{
		if (this.running.compareAndSet(true, false))
		{
			try
			{
				this.synchronizer.close();
			}
			catch (final IOException e)
			{
				LOG.error("Error closing synchronizer", e);
			}
		}
	}

	@Override
	public boolean isRunning()
	{
		return this.running.get();
	}

	@Override
	protected Message<File> doReceive()
	{
		Assert.state(this.running.get(), this.getComponentName() + " is not running");
		final AzureBlobFileInfo file = poll();
		if (file != null)
		{
			return getMessageBuilderFactory()
					.withPayload(new File(file.getLocalDirectory(), file.getFilename()))
					.setHeader(RemoteFileHeaders.REMOTE_DIRECTORY, file.getRemoteDirectory())
					.setHeader(RemoteFileHeaders.REMOTE_FILE, file.getFilename())
					.setHeader(RemoteFileHeaders.REMOTE_FILE_DELETED, file.isDeleted())
					.build();
		}
		return null;
	}


	protected AzureBlobFileInfo poll()
	{
		if (this.toBeReceived.isEmpty())
		{
			synchronizeFiles();
		}
		return this.toBeReceived.poll();
	}

	protected void synchronizeFiles()
	{
		final List<AzureBlobFileInfo> azureBlobFileInfos
				= this.synchronizer.synchronizeToLocalDirectoryAndGetFileInfo(localDirectory, getMaxFetchSize());
		this.toBeReceived.addAll(azureBlobFileInfos);
	}

}
