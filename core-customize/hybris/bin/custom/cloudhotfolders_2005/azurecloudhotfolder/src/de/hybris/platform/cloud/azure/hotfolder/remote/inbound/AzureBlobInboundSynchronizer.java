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
package de.hybris.platform.cloud.azure.hotfolder.remote.inbound;

import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import de.hybris.platform.cloud.azure.hotfolder.remote.session.AzureBlobFileInfo;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.InboundFileSynchronizer;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;

/**
 * {@link AbstractInboundFileSynchronizer} implementation to allow synchronizing files from a Blob Storage Account.
 * Ensures Remote Directory Expression is set to use the Directory Delimiter of the Client
 *
 */
@SuppressWarnings("unused")
public class AzureBlobInboundSynchronizer implements InboundFileSynchronizer, BeanFactoryAware, InitializingBean, Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(AzureBlobInboundSynchronizer.class);

    protected static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final RemoteFileTemplate<CloudBlob> remoteFileTemplate;

    private EvaluationContext evaluationContext;

    private String remoteFileSeparator = "/";

    /**
     * Extension used when downloading files. We change it right after we know it's downloaded.
     */
    private String temporaryFileSuffix = ".writing";

    private Expression localFilenameGeneratorExpression;

    /**
     * the path on the remote mount as a String.
     */
    private Expression remoteDirectoryExpression;

    /**
     * The current evaluation of the expression.
     */
    private volatile String evaluatedRemoteDirectory;

    /**
     * An {@link FileListFilter} that runs against the <em>remote</em> file system view.
     */
    private FileListFilter<CloudBlob> filter;

    /**
     * Should we <em>delete</em> the remote <b>source</b> files
     * after copying to the local directory? By default this is false.
     */
    private boolean deleteRemoteFiles = false;

    /**
     * the path on the remote mount to move files to as a String.
     */
    private Expression moveToRemoteDirectoryExpression;

    /**
     * The current evaluation of the move-to expression.
     */
    private volatile String evaluatedMoveToRemoteDirectory;

    /**
     * Should we <em>transfer</em> the remote file <b>timestamp</b>
     * to the local file? By default this is false.
     */
    private boolean preserveTimestamp;

    private BeanFactory beanFactory;

    private Comparator<CloudBlob> comparator;

    public AzureBlobInboundSynchronizer(final SessionFactory<CloudBlob> sessionFactory)
    {
        Assert.notNull(sessionFactory, "sessionFactory must not be null");
        this.remoteFileTemplate = new RemoteFileTemplate<>(sessionFactory);
        final CloudBlobClient client = (CloudBlobClient) sessionFactory.getSession().getClientInstance();
        setRemoteDirectoryExpression(new LiteralExpression(client.getDirectoryDelimiter()));
    }

    protected Comparator<CloudBlob> getComparator()
    {
        return this.comparator;
    }

    /**
     * Set a comparator to sort the retrieved list of {@code CloudBlob} (the Type that represents
     * the remote file) prior to applying filters and max fetch size.
     *
     * @param comparator the comparator.
     * @since 5.1
     */
    public void setComparator(final Comparator<CloudBlob> comparator)
    {
        this.comparator = comparator;
    }


    /**
     * @param remoteFileSeparator the remote file separator.
     * @see RemoteFileTemplate#setRemoteFileSeparator(String)
     */
    public void setRemoteFileSeparator(final String remoteFileSeparator)
    {
        Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
        this.remoteFileSeparator = remoteFileSeparator;
    }

    /**
     * Set an expression used to determine the local file name.
     *
     * @param localFilenameGeneratorExpression the expression.
     */
    public void setLocalFilenameGeneratorExpression(final Expression localFilenameGeneratorExpression)
    {
        Assert.notNull(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be null");
        this.localFilenameGeneratorExpression = localFilenameGeneratorExpression;
    }

    /**
     * Set an expression used to determine the local file name.
     *
     * @param localFilenameGeneratorExpression the expression.
     * @see #setRemoteDirectoryExpression(Expression)
     * @since 4.3.13
     */
    public void setLocalFilenameGeneratorExpressionString(final String localFilenameGeneratorExpression)
    {
        setLocalFilenameGeneratorExpression(EXPRESSION_PARSER.parseExpression(localFilenameGeneratorExpression));
    }

    /**
     * Set a temporary file suffix to be used while transferring files. Default ".writing".
     *
     * @param temporaryFileSuffix the file suffix.
     */
    @SuppressWarnings("unused")
    public void setTemporaryFileSuffix(final String temporaryFileSuffix)
    {
        this.temporaryFileSuffix = temporaryFileSuffix;
    }

    /**
     * Specify the full path to the remote directory.
     *
     * @param remoteDirectory The remote directory.
     */
    public void setRemoteDirectory(final String remoteDirectory)
    {
        this.remoteDirectoryExpression = new LiteralExpression(remoteDirectory);
        evaluateRemoteDirectory();
    }

    /**
     * Specify an expression that evaluates to the full path to the remote directory.
     *
     * @param remoteDirectoryExpression The remote directory expression.
     * @since 4.2
     */
    public void setRemoteDirectoryExpression(final Expression remoteDirectoryExpression)
    {
        doSetRemoteDirectoryExpression(remoteDirectoryExpression);
    }

    /**
     * Specify an expression that evaluates to the full path to the remote directory.
     *
     * @param remoteDirectoryExpression The remote directory expression.
     * @see #setRemoteDirectoryExpression(Expression)
     * @since 4.3.13
     */
    public void setRemoteDirectoryExpressionString(final String remoteDirectoryExpression)
    {
        doSetRemoteDirectoryExpression(EXPRESSION_PARSER.parseExpression(remoteDirectoryExpression));
    }


    protected final void doSetRemoteDirectoryExpression(final Expression remoteDirectoryExpression)
    {
        Assert.notNull(remoteDirectoryExpression, "'remoteDirectoryExpression' must not be null");
        this.remoteDirectoryExpression = remoteDirectoryExpression;
        evaluateRemoteDirectory();
    }

    /**
     * Set the filter to be applied to the remote files before transferring.
     *
     * @param filter the file list filter.
     */
    public void setFilter(final FileListFilter<CloudBlob> filter)
    {
        doSetFilter(filter);
    }

    protected final void doSetFilter(final FileListFilter<CloudBlob> filter)
    {
        this.filter = filter;
    }

    /**
     * Set to true to enable deletion of remote files after successful transfer.
     *
     * @param deleteRemoteFiles true to delete.
     */
    public void setDeleteRemoteFiles(final boolean deleteRemoteFiles)
    {
        this.deleteRemoteFiles = deleteRemoteFiles;
    }

    /**
     * Set to true to enable the preservation of the remote file timestamp when
     * transferring.
     *
     * @param preserveTimestamp true to preserve.
     */
    public void setPreserveTimestamp(final boolean preserveTimestamp)
    {
        this.preserveTimestamp = preserveTimestamp;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    /**
     * Specify the full path to the move-to remote directory.
     *
     * @param moveToRemoteDirectory The remote directory.
     */
    public void setMoveToRemoteDirectory(final String moveToRemoteDirectory)
    {
        this.moveToRemoteDirectoryExpression = new LiteralExpression(moveToRemoteDirectory);
        evaluateMoveToRemoteDirectory();
    }

    /**
     * Specify an expression that evaluates to the full path to the move-to remote directory.
     *
     * @param moveToRemoteDirectoryExpression The remote directory expression.
     * @since 4.2
     */
    public void setMoveToRemoteDirectoryExpression(final Expression moveToRemoteDirectoryExpression)
    {
        doSetMoveToRemoteDirectoryExpression(moveToRemoteDirectoryExpression);
    }

    /**
     * Specify an expression that evaluates to the full path to the move-to  remote directory.
     *
     * @param moveToRemoteDirectoryExpression The remote directory expression.
     * @see #setRemoteDirectoryExpression(Expression)
     * @since 4.3.13
     */
    public void setMoveToRemoteDirectoryExpressionString(final String moveToRemoteDirectoryExpression)
    {
        doSetMoveToRemoteDirectoryExpression(EXPRESSION_PARSER.parseExpression(moveToRemoteDirectoryExpression));
    }


    protected final void doSetMoveToRemoteDirectoryExpression(final Expression moveToRemoteDirectoryExpression)
    {
        Assert.notNull(moveToRemoteDirectoryExpression, "'moveToRemoteDirectoryExpression' must not be null");
        this.moveToRemoteDirectoryExpression = remoteDirectoryExpression;
        evaluateMoveToRemoteDirectory();
    }


    @Override
    public final void afterPropertiesSet()
    {
        Assert.state(this.remoteDirectoryExpression != null, "'remoteDirectoryExpression' must not be null");
        if (this.evaluationContext == null)
        {
            this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
        }
        evaluateRemoteDirectory();
        evaluateMoveToRemoteDirectory();
        if (deleteRemoteFiles)
        {
            Assert.state(!StringUtils.isEmpty(this.evaluatedRemoteDirectory),
                    "'moveToRemoteDirectory' and 'deleteRemoteFiles' are mutually exclusive.");
        }

    }

    protected final List<CloudBlob> filterFiles(final CloudBlob[] files)
    {
        return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
    }

    @SuppressWarnings("unchecked")
    public static <F> F[] purgeUnwantedElements(final F[] fileArray,
                                                final Predicate<F> predicate,
                                                final Comparator<F> comparator)
    {
        if (ObjectUtils.isEmpty(fileArray))
        {
            return fileArray;
        }
        else
        {
            if (comparator == null)
            {
                return Arrays.stream(fileArray)
                        .filter(predicate.negate())
                        .toArray(size -> (F[]) Array.newInstance(fileArray[0].getClass(), size));
            }
            else
            {
                return Arrays.stream(fileArray)
                        .filter(predicate.negate())
                        .sorted(comparator)
                        .toArray(size -> (F[]) Array.newInstance(fileArray[0].getClass(), size));
            }
        }
    }

    protected String getTemporaryFileSuffix()
    {
        return this.temporaryFileSuffix;
    }

    @Override
    public void close() throws IOException
    {
        if (this.filter instanceof Closeable)
        {
            ((Closeable) this.filter).close();
        }
    }

    @Override
    public void synchronizeToLocalDirectory(final File localDirectory)
    {
	    synchronizeToLocalDirectoryAndGetFileInfo(localDirectory, Integer.MIN_VALUE);
    }

    @Override
    public void synchronizeToLocalDirectory(final File localDirectory, final int maxFetchSize)
    {
	    synchronizeToLocalDirectoryAndGetFileInfo(localDirectory, maxFetchSize);
    }

    public List<AzureBlobFileInfo> synchronizeToLocalDirectoryAndGetFileInfo(final File localDirectory, final int maxFetchSize)
    {
        if (maxFetchSize == 0)
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Max Fetch Size is zero - fetch to " + localDirectory.getAbsolutePath() + " ignored");
            }
            return Lists.emptyList();
        }
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Synchronizing " + this.evaluatedRemoteDirectory + " to " + localDirectory);
        }
        try
        {
            final List<AzureBlobFileInfo> azureBlobFileInfos = this.remoteFileTemplate.execute(session ->
                    transferFilesFromRemoteToLocal(localDirectory, maxFetchSize, session));
            if (LOG.isDebugEnabled())
            {
                final int count = (azureBlobFileInfos == null) ? 0 : azureBlobFileInfos.size();
                LOG.debug(count + " files transferred from '" + this.evaluatedRemoteDirectory + "'");
            }
            return azureBlobFileInfos;
        }
        catch (final Exception e)
        {
            throw new MessagingException("Problem occurred while synchronizing '"
                    + this.evaluatedRemoteDirectory + "' to local directory", e);
        }
    }

    // Suppress Sonar warnings - this is mostly spring code copied here to extend and fix bugs.
    // I don't want to change it's structure as it will make it much more difficult to compare to the
    // original in the future.
    @SuppressWarnings({"squid:S3776", "squid:S134"})
    private List<AzureBlobFileInfo> transferFilesFromRemoteToLocal(final File localDirectory, final int maxFetchSize,
                                                                   final Session<CloudBlob> session)
            throws IOException
    {

        CloudBlob[] files = session.list(this.evaluatedRemoteDirectory);
        if (!ObjectUtils.isEmpty(files))
        {
            files = purgeUnwantedElements(files, e -> !isFile(e), this.comparator);
        }

        if (!ObjectUtils.isEmpty(files))
        {
            List<CloudBlob> filteredFiles = filterFiles(files);
            if (maxFetchSize >= 0 && filteredFiles.size() > maxFetchSize)
            {
                rollbackFromFileToListEnd(filteredFiles, filteredFiles.get(maxFetchSize));
                final List<CloudBlob> newList = new ArrayList<>(maxFetchSize);
                for (int i = 0; i < maxFetchSize; i++)
                {
                    newList.add(filteredFiles.get(i));
                }
                filteredFiles = newList;
            }

            final List<AzureBlobFileInfo> copiedFiles = new ArrayList<>(filteredFiles.size());
            for (final CloudBlob file : filteredFiles)
            {
                try
                {
                    if (file != null)
                    {
                        final AzureBlobFileInfo azureBlobFileInfo =
                                copyFileToLocalDirectory(this.evaluatedRemoteDirectory, file, localDirectory, session);
                        if (azureBlobFileInfo != null)
                        {
                            copiedFiles.add(azureBlobFileInfo);
                        }
                    }
                }
                catch (final RuntimeException | IOException e1)
                {
                    rollbackFromFileToListEnd(filteredFiles, file);
                    throw e1;
                }
            }
            return copiedFiles;
        }
        else
        {
            return Lists.emptyList();
        }
    }

    protected void rollbackFromFileToListEnd(final List<CloudBlob> filteredFiles, final CloudBlob file)
    {
        if (this.filter instanceof ReversibleFileListFilter)
        {
            ((ReversibleFileListFilter<CloudBlob>) this.filter)
                    .rollback(file, filteredFiles);
        }
    }

    // Suppress Sonar warnings - this is mostly spring code copied here to extend and fix bugs.
    // I don't want to change it's structure as it will make it much more difficult to compare to the
    // original in the future.
    @SuppressWarnings({"squid:S134"})
    protected AzureBlobFileInfo copyFileToLocalDirectory(final String remoteDirectoryPath, final CloudBlob remoteFile,// NOSONAR
                                                         final File localDirectory, final Session<CloudBlob> session)
            throws IOException
    {

        final String remoteFileName = getFilename(remoteFile);
        final String localFileName = generateLocalFileName(remoteFileName);
        final String remoteFilePath = remoteDirectoryPath != null
                ? (remoteDirectoryPath + this.remoteFileSeparator + remoteFileName)
                : remoteFileName;

        if (!isFile(remoteFile))
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("cannot copy, not a file: " + remoteFilePath);
            }
            return null;
        }

        final long modified = getModified(remoteFile);

        final File localFile = new File(localDirectory, localFileName);
        final boolean exists = localFile.exists();

        final AzureBlobFileInfo azureBlobFileInfo = new AzureBlobFileInfo(remoteFile, localDirectory.getCanonicalPath());

        if (!exists || (this.preserveTimestamp && modified != localFile.lastModified()))
        {
            if (!exists &&
                    localFileName.replaceAll("/", Matcher.quoteReplacement(File.separator)).contains(File.separator))
            {
                //noinspection ResultOfMethodCallIgnored
                localFile.getParentFile().mkdirs(); //NOSONAR - will fail on the writing below
            }

            boolean transfer = true;

            if (exists)
            {
                try
                {
                    Files.delete(localFile.toPath());
                }
                catch (final IOException ex)
                {
                    transfer = false;
                    if (LOG.isInfoEnabled())
                    {
                        LOG.info("Cannot delete local file '" + localFile +
                                "' in order to transfer modified remote file '" + remoteFile + "'. " +
                                "The local file may be busy in some other process.", ex);
                    }
                }
            }

            boolean renamed = false;

            if (transfer)
            {
                renamed = copyRemoteContentToLocalFile(session, remoteFilePath, localFile);
            }

            if (renamed)
            {
                if (!StringUtils.isEmpty(this.evaluatedMoveToRemoteDirectory))
                {
                    azureBlobFileInfo.setRemoteDirectory(this.evaluatedMoveToRemoteDirectory);
                    final String moveToFilePath
                            = this.evaluatedMoveToRemoteDirectory + this.remoteFileSeparator + remoteFileName;
                    session.rename(remoteFilePath, moveToFilePath);
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("moved remote file: " + remoteFilePath + " to " + moveToFilePath);
                    }
                }
                else if (this.deleteRemoteFiles)
                {
                    azureBlobFileInfo.setDeleted(true);
                    session.remove(remoteFilePath);
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("deleted remote file: " + remoteFilePath);
                    }
                }
                if (this.preserveTimestamp && !localFile.setLastModified(modified))
                {
                    throw new IllegalStateException("Could not sent last modified on file: " + localFile);
                }
                return azureBlobFileInfo;
            }
            else if (this.filter instanceof ResettableFileListFilter)
            {
                if (LOG.isInfoEnabled())
                {
                    LOG.info("Reverting the remote file '" + remoteFile +
                            "' from the filter for a subsequent transfer attempt");
                }
                ((ResettableFileListFilter<CloudBlob>) this.filter).remove(remoteFile);
            }
        }
        else if (LOG.isWarnEnabled())
        {
            LOG.warn("The remote file '" + remoteFile + "' has not been transferred " +
                    "to the existing local file '" + localFile + "'. Consider removing the local file.");
        }

        return null;
    }

    // Suppress Sonar warnings - this is mostly spring code copied here to extend and fix bugs.
    // I don't want to change it's structure as it will make it much more difficult to compare to the
    // original in the future.
    @SuppressWarnings({"squid:S2221"})
    private boolean copyRemoteContentToLocalFile(final Session<CloudBlob> session, final String remoteFilePath,
                                                 final File localFile)
    {
        boolean renamed;
        final String tempFileName = localFile.getAbsolutePath() + this.temporaryFileSuffix;
        final File tempFile = new File(tempFileName);

        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile)))
        {
            session.read(remoteFilePath, outputStream);
        }
        catch (final RuntimeException re)
        {
            throw re;
        }
        catch (final Exception e)
        {
            throw new MessagingException("Failure occurred while copying '" + remoteFilePath
                    + "' from the remote to the local directory", e);
        }

        renamed = tempFile.renameTo(localFile);

        if (!renamed)
        {
            try
            {
                Files.delete(localFile.toPath());
                renamed = tempFile.renameTo(localFile);
                if (!renamed && LOG.isInfoEnabled())
                {
                    LOG.info("Cannot rename '"
                            + tempFileName
                            + "' to local file '" + localFile + "' after deleting. " +
                            "The local file may be busy in some other process.");
                }
            }
            catch (final IOException ex)
            {
                if (LOG.isInfoEnabled())
                {
                    LOG.info("Cannot delete local file '" + localFile +
                            "'. The local file may be busy in some other process.", ex);
                }
            }
        }
        return renamed;
    }

    private String generateLocalFileName(final String remoteFileName)
    {
        if (this.localFilenameGeneratorExpression != null)
        {
            return this.localFilenameGeneratorExpression.getValue(this.evaluationContext, remoteFileName,
                    String.class);
        }
        return remoteFileName;
    }

    protected void evaluateRemoteDirectory()
    {
        if (this.evaluationContext != null)
        {
            this.evaluatedRemoteDirectory = this.remoteDirectoryExpression.getValue(this.evaluationContext,
                    String.class);
            this.evaluationContext.setVariable("remoteDirectory", this.evaluatedRemoteDirectory);

        }
    }

    protected void evaluateMoveToRemoteDirectory()
    {
        if (this.evaluationContext != null)
        {
            this.evaluatedMoveToRemoteDirectory = this.moveToRemoteDirectoryExpression.getValue(this.evaluationContext,
                    String.class);
            this.evaluationContext.setVariable("moveToRemoteDirectory", this.evaluatedMoveToRemoteDirectory);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isFile(final CloudBlob entry)
    {
        return entry != null;
    }

    private String getFilename(final CloudBlob entry)
    {
        return AzureBlobFileInfo.getFilename(entry);
    }

    private long getModified(final CloudBlob entry)
    {
        return AzureBlobFileInfo.getModified(entry);
    }

}
