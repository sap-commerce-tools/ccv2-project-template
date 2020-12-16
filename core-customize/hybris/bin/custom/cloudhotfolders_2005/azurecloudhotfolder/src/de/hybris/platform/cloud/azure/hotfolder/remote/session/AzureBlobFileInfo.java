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
package de.hybris.platform.cloud.azure.hotfolder.remote.session;

import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlob;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;

/**
 * A {@link FileInfo} implementation for Azure Storage Blobs
 *
 **/
public class AzureBlobFileInfo implements FileInfo<CloudBlob>, Comparable<FileInfo<CloudBlob>>
{

    private final CloudBlob cloudBlob;
    private final String localDirectory;

    private String remoteDirectory;
    private boolean isDeleted = false;

    public AzureBlobFileInfo(final CloudBlob azureBlobEntry, final String localDirectory)
    {
        Assert.notNull(azureBlobEntry, "AzureBlobEntry must not be null");
        Assert.notNull(localDirectory, "LocalFilePath must not be null");
        this.cloudBlob = azureBlobEntry;
        this.localDirectory = localDirectory;
        this.remoteDirectory = getRemoteDirectory(cloudBlob);
    }

    @Override
    public boolean isDirectory()
    {
        return isDirectory(this.cloudBlob);  // All cloud blobs are files.
    }

    @Override
    public boolean isLink()
    {
        return false; // All cloud blobs are files.
    }

    public String getId()
    {
        return this.cloudBlob.getUri().toString();
    }

    @Override
    public long getSize()
    {
        return getSize(this.cloudBlob);
    }

    @Override
    public long getModified()
    {
        return getModified(cloudBlob);
    }

    @Override
    public String getFilename()
    {
        return getFilename(this.cloudBlob);
    }

    @Override
    public String getPermissions()
    {
        return null;
    }

    public String getLocalDirectory()
    {
        return this.localDirectory;
    }

    public String getRemoteDirectory()
    {
        return this.remoteDirectory;
    }

    public void setRemoteDirectory(final String remoteDirectory)
    {
        this.remoteDirectory = remoteDirectory;
    }

    public String getOriginalRemoteDirectory()
    {
        return getRemoteDirectory(this.cloudBlob);
    }

    public boolean isDeleted()
    {
        return this.isDeleted;
    }

    public void setDeleted(final boolean deleted)
    {
        this.isDeleted = deleted;
    }

    @Override
    public CloudBlob getFileInfo()
    {
        return this.cloudBlob;
    }


    /**
     * Returns false as all cloud blobs are file
     *
     * @param cloudBlob the cloud blob
     * @return false
     */
    public static boolean isDirectory(final CloudBlob cloudBlob)
    {
        return false;
    }

    /**
     * Returns the last modified timestamp for a given cloud blob.
     *
     * @param cloudBlob the cloud blob
     * @return returns the last modified timestamp, if available; otherwise {@link Long#MIN_VALUE}.
     */
    public static long getModified(final CloudBlob cloudBlob)
    {
        return Optional.of(cloudBlob)
                .map(CloudBlob::getProperties)
                .map(BlobProperties::getLastModified)
                .map(Date::getTime)
                .orElse(Long.MIN_VALUE);
    }

    /**
     * Returns the filesize, in bytes, for a given cloud blob.
     *
     * @param cloudBlob the cloud blob
     * @return returns the file size, if available; otherwise 0.
     */
    public static long getSize(final CloudBlob cloudBlob)
    {
        return Optional.of(cloudBlob)
                .map(CloudBlob::getProperties)
                .map(BlobProperties::getLength)
                .orElse(0L);
    }

    /**
     * Returns the remote file name, excluding the path, for a given cloud blob.
     *
     * @param cloudBlob the cloud blob
     * @return returns the remote file name, if available; otherwise an empty string.
     */
    public static String getFilename(final CloudBlob cloudBlob)
    {
        return Optional.of(cloudBlob)
                .map(CloudBlob::getName)
                .map(Paths::get)
                .map(Path::getFileName)
                .map(Path::toString)
                .orElse("");
    }

    /**
     * Returns the remote directory, excluding the filename, for a given cloud blob.
     *
     * @param cloudBlob the cloud blob
     * @return returns the remote directory, if available; otherwise an empty string.
     */
    public static String getRemoteDirectory(final CloudBlob cloudBlob)
    {
        return Optional.of(cloudBlob)
                .map(CloudBlob::getName)
                .map(Paths::get)
                .map(Path::getParent)
                .map(Path::toString)
                .orElse("");
    }

    /**
     * Returns the remote path, including the filename, for a given cloud blob.
     *
     * @param cloudBlob the cloud blob
     * @return returns the remote path, if available; otherwise an empty string.
     */
    public static String getRemotePath(final CloudBlob cloudBlob)
    {
        return Optional.of(cloudBlob)
                .map(CloudBlob::getName)
                .orElse("");
    }

    public int compareTo(final FileInfo<CloudBlob> o)
    {
        return this.getFilename().compareTo(o.getFilename());
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj != null && obj.getClass() == this.getClass())
        {
            final AzureBlobFileInfo c = (AzureBlobFileInfo) obj;
            return c.getFilename().equals(this.getFilename());
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return this.getFilename().hashCode();
    }
}
