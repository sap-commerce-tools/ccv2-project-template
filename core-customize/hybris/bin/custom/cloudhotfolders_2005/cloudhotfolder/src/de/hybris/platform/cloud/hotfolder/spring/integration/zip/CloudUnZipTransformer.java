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
package de.hybris.platform.cloud.hotfolder.spring.integration.zip;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.zip.transformer.AbstractZipTransformer;
import org.springframework.integration.zip.transformer.SpringZipUtils;
import org.springframework.integration.zip.transformer.ZipResultType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * UnZip Transformer, unzips an archive to a working directory and
 * returns a message containing a map of the files within.
 *
 * A rewrite of {@link org.springframework.integration.zip.transformer.UnZipTransformer} that
 * adds additional headers containing the root of the unzipped files and the path to the original
 * archive.
 */
// Suppress Sonar warnings - this is mostly spring code copied here to extend and fix bugs.
// I don't want to change it's structure as it will make it much more difficult to compare to the
// original in the future.
@SuppressWarnings({"squid:S134"})
public class CloudUnZipTransformer extends AbstractZipTransformer
{
    private static final Logger LOG = getLogger(CloudUnZipTransformer.class);


    private volatile boolean expectSingleResult = false;

    public void setExpectSingleResult(final boolean expectSingleResult)
    {
        this.expectSingleResult = expectSingleResult;
    }

    @Override
    protected Object doZipTransform(final Message<?> message)
    {
        try
        {
            final Object payload = message.getPayload();
            final File unZipDirectory = getUnZipDirectory(message, workDirectory);
            final Object result;

            try (final InputStream inputStream = getInputStream(payload))
            {
                final SortedMap<String, Object> uncompressedData = new TreeMap<>();

                ZipUtil.iterate(inputStream, (zipEntryInputStream, zipEntry) -> {

                    logZipEntry(zipEntry);
                    final String zipEntryName = zipEntry.getName();

                    if (ZipResultType.FILE.equals(zipResultType))
                    {
                        final File destinationFile = getValidDestinationFile(unZipDirectory, zipEntryName);

                        if (zipEntry.isDirectory())
                        {
                            //noinspection ResultOfMethodCallIgnored
                            destinationFile.mkdirs(); //NOSONAR false positive
                        }
                        else
                        {
                            SpringZipUtils.copy(zipEntryInputStream, destinationFile);
                            uncompressedData.put(zipEntryName, destinationFile);
                        }
                    }
                    else if (ZipResultType.BYTE_ARRAY.equals(zipResultType))
                    {
                        if (!zipEntry.isDirectory())
                        {
                            // Check that the zipEntryName would be valid if we
                            // were to create a file for it.
                            getValidDestinationFile(unZipDirectory, zipEntryName);
                            final byte[] data = IOUtils.toByteArray(zipEntryInputStream);
                            uncompressedData.put(zipEntryName, data);
                        }
                    }
                    else
                    {
                        throw new IllegalStateException("Unsupported zipResultType " + zipResultType);
                    }

                });

                result = getResult(message, uncompressedData);
            }
            finally
            {
                if (this.deleteFiles)
                {
                    deletePayload(payload);
                }
            }

            final AbstractIntegrationMessageBuilder<Object> builder = getMessageBuilderFactory()
                    .withPayload(result)
                    .copyHeaders(message.getHeaders());

            if (payload instanceof File)
            {
                builder.setHeader(CloudZipHeaders.ARCHIVE_PATH, ((File) payload).getCanonicalPath());
            }

            if(ZipResultType.FILE.equals(zipResultType))
            {
                builder.setHeader(CloudZipHeaders.UNZIP_DIRECTORY, unZipDirectory);
            }

            return builder.build();

        }
        catch (final Exception e)
        {
            throw new MessageHandlingException(message, "Failed to apply Zip transformation.", e);
        }
    }

    public File getValidDestinationFile(final File unzipDirectory, final String zipEntryName) throws IOException
    {
        //noinspection ResultOfMethodCallIgnored
        unzipDirectory.mkdirs(); //NOSONAR false positive

        final File destinationFile = new File(unzipDirectory, zipEntryName);

        /* If we see the relative traversal string of ".." we need to make sure
         * that the outputdir + name doesn't leave the outputdir.
         */
        if (!destinationFile.getCanonicalPath().startsWith(workDirectory.getCanonicalPath()))
        {
            throw new ZipException("The file " + zipEntryName +
                    " is trying to leave the target output directory of " + workDirectory);
        }
        return destinationFile;
    }

    protected void logZipEntry(final ZipEntry zipEntry)
    {
        if (LOG.isInfoEnabled())
        {
            final String zipEntryName = zipEntry.getName();
            final String type = zipEntry.isDirectory() ? "directory" : "file";
            final long zipEntryTime = zipEntry.getTime();
            final long zipEntryCompressedSize = zipEntry.getCompressedSize();
            LOG.info(String.format("Unpacking Zip Entry - Name: '%s',Time: '%s', " +
                            "Compressed Size: '%s', Type: '%s'",
                    zipEntryName, zipEntryTime, zipEntryCompressedSize, type));
        }
    }

    protected File getUnZipDirectory(final Message<?> message, final File workDirectory)
    {
        return new File(workDirectory, message.getHeaders().getId().toString());
    }

    protected InputStream getInputStream(final Object payload) throws FileNotFoundException
    {
        final InputStream inputStream;
        if (payload instanceof File)
        {
            final File filePayload = (File) payload;

            if (filePayload.isDirectory())
            {
                throw new UnsupportedOperationException(String.format("Cannot unzip a directory: '%s'",
                        filePayload.getAbsolutePath()));
            }

            if (!isValid(filePayload))
            {
                throw new IllegalStateException(String.format("Not a zip file: '%s'.",
                        filePayload.getAbsolutePath()));
            }

            inputStream = new FileInputStream(filePayload);
        }
        else if (payload instanceof InputStream)
        {
            inputStream = (InputStream) payload;
        }
        else if (payload instanceof byte[])
        {
            inputStream = new ByteArrayInputStream((byte[]) payload);
        }
        else
        {
            throw new IllegalArgumentException(String.format("Unsupported payload type '%s'. " +
                            "The only supported payload types are java.io.File, byte[] and java.io.InputStream",
                    payload.getClass().getSimpleName()));
        }
        return inputStream;
    }

    protected Object getResult(final Message<?> message, final SortedMap<String, Object> uncompressedData)
    {
        if (uncompressedData.isEmpty())
        {
            if (LOG.isWarnEnabled())
            {
                LOG.warn("No data unzipped from payload with message Id " + message.getHeaders().getId());
            }
            return null;
        }
        else
        {
            if (this.expectSingleResult)
            {
                if (uncompressedData.size() == 1)
                {
                    return uncompressedData.values().iterator().next();
                }
                else
                {
                    throw new MessagingException(message,
                            String.format("The UnZip operation extracted %s "
                                    + "result objects but expectSingleResult was 'true'.", uncompressedData.size()));
                }
            }
            else
            {
                return uncompressedData;
            }

        }
    }

    protected void deletePayload(final Object payload)
    {
        if (payload instanceof File)
        {
            final File filePayload = (File) payload;
            try
            {
                Files.delete(filePayload.toPath());
            }
            catch (final IOException ex)
            {
                if (LOG.isWarnEnabled())
                {
                    LOG.warn("Failed to delete File '" + filePayload + "'", ex);
                }
            }
        }
    }

    protected static boolean isValid(final File file)
    {
        try (final ZipFile zipfile = new ZipFile(file))
        {
            return true;
        }
        catch (final IOException | RuntimeException ex)
        {
            LOG.debug("Error testing validity of archive [{}].", file, ex);
            return false;
        }
    }


}
