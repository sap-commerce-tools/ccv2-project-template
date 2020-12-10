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
package de.hybris.platform.cloud.azure.hotfolder.remote.session;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class TestBlobSession implements ExtendedSession<CloudBlob>
{
    private static final Logger LOG = getLogger(TestBlobSession.class);

    private final CloudBlobClient client;
    private final String remoteDirectory;
    private final Map<String, File> files = new HashMap<>();
    private final List<String> filesRead = new LinkedList<>();

    public TestBlobSession(final CloudBlobClient client, final String remoteDirectory)
    {
        Assert.notNull(client, "client must not be null");
        Assert.notNull(remoteDirectory, "remoteDirectory must not be null");
        this.client = client;
        this.remoteDirectory = remoteDirectory;
        clean();
    }

    //<editor-fold desc="Test helper methods">

    public void setFiles(final List<File> files)
    {
        for (final File file : files)
        {
            this.files.put(remoteDirectory + File.separator + file.getName(), file);
        }
        LOG.debug("Setting session to return files {}", this.files);
    }

    public boolean fileRead(final String path)
    {
        LOG.debug("Checking if path [{}] was logged as read in {}", path, this.filesRead);
        return this.filesRead.stream().anyMatch(read -> read.endsWith(path));
    }

    public void clean()
    {
        this.files.clear();
        this.filesRead.clear();
    }

    //</editor-fold>

    //<editor-fold description="Methods implemented for test class">

    @Override
    public CloudBlob[] list(final String path)
    {
        LOG.debug("Polling for [{}]", path);
        return files.values().stream()
                .map(this::createBlockBlob)
                .toArray(CloudBlob[]::new);
    }

    @Override
    public void read(final String path, final OutputStream out) throws IOException
    {
        LOG.debug("Reading [{}]", path);
        final File file = files.get(path);
        if (file == null)
        {
            throw new FileNotFoundException("File not found: " + path);
        }
        FileUtils.copyFile(file, out);
        this.filesRead.add(path);
    }

    @Override
    public CloudBlob get(final String path) throws IOException
    {
        LOG.debug("Get [{}]", path);
        final File file = files.get(path);
        if (file == null)
        {
            throw new FileNotFoundException("File not found: " + path);
        }
        return createBlockBlob(file);
    }


    public CloudBlob createBlockBlob(final File file)
    {
        try
        {
            final BlobProperties entryProperties = new BlobProperties();
            ReflectionTestUtils.setField(entryProperties, "lastModified", new Date(file.lastModified()));
            ReflectionTestUtils.setField(entryProperties, "length", 200);

            final String path = file.getPath();
            final CloudBlockBlob cloudBlockBlob = new CloudBlockBlob(new URI("http", "myserver", path, null));
            ReflectionTestUtils.setField(cloudBlockBlob, "properties", entryProperties);

            return cloudBlockBlob;
        }
        catch (final URISyntaxException | StorageException ex)
        {
            return null;
        }
    }

    @Override
    public boolean remove(final String path)
    {
        LOG.debug("Removing [{}]", path);
        return true;
    }

    @Override
    public Object getClientInstance()
    {
        return client;
    }

    //</editor-fold>

    //<editor-fold description="Stuff don't need to worry about implementing for test">
    @Override
    public void write(final InputStream inputStream, final String destination)
    {

    }

    @Override
    public void append(final InputStream inputStream, final String destination)
    {

    }

    @Override
    public boolean mkdir(final String directory)
    {
        return false;
    }

    @Override
    public boolean rmdir(final String directory)
    {
        return false;
    }

    @Override
    public void rename(final String pathFrom, final String pathTo)
    {

    }

    @Override
    public void close()
    {

    }

    @Override
    public boolean isOpen()
    {
        return false;
    }

    @Override
    public boolean exists(final String path)
    {
        return false;
    }

    @Override
    public String[] listNames(final String path)
    {
        return new String[0];
    }

    @Override
    public InputStream readRaw(final String source)
    {
        return null;
    }

    @Override
    public boolean finalizeRaw()
    {
        return false;
    }

    @Override
    public String getHostPort() {
        return "localhost:8080";
    }

    //</editor-fold>
}
