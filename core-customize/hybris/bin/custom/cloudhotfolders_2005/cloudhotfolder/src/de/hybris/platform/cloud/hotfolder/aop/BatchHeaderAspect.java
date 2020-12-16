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
package de.hybris.platform.cloud.hotfolder.aop;

import de.hybris.platform.acceleratorservices.dataimport.batch.BatchHeader;
import de.hybris.platform.cloud.commons.aop.exception.StepException;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.hotfolder.aop.utils.ActionCodeGenerator;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.MultiMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * AOP implementation to monitor the various stages of a Hot Folder Spring Integration Message
 */
// Suppress Sonar warnings - AOP aspects are supposed to throw Throwables.
@SuppressWarnings({"squid:S00112"})
public class BatchHeaderAspect extends AbstractMonitoringAspect
{
	private static final Logger LOG = getLogger(BatchHeaderAspect.class);
	private String fileNameHeaderKey;

	/**
	 * AOP Around implementation to monitor the setup of a Header based on a File
	 * @param pjp  Pointcut to be executed
	 * @param file the File the header is being setup for
	 * @return the value from the execution
	 * @throws Throwable any exception/error thrown during set up of the aspect for logging or a {@link StepException} in the
	 *                   case of an exception during execution of the method call
	 */
	public Object aroundFileSetup(final ProceedingJoinPoint pjp, final File file) throws Throwable
	{
		final String fileName = file.getName();
		resumeMonitor(SystemArea.HOT_FOLDER, fileName + file.lastModified());
		return aroundSetup(pjp, () -> fileName);
	}

	/**
	 * AOP Around implementation to monitor the setup of a Header based on a Message
	 * @param pjp     Pointcut to be executed
	 * @param message the Message the header is being setup for
	 * @return the value from the execution
	 * @throws Throwable any exception thrown by the method called
	 */
	public Object aroundMessageSetup(final ProceedingJoinPoint pjp, final Message<?> message) throws Throwable
	{
		return aroundSetup(pjp, () -> {
			final MessageHeaders headers = message.getHeaders();
			return (String) headers.get(getFileNameHeaderKey());
		});
	}

	private Object aroundSetup(final ProceedingJoinPoint pjp, final Supplier<String> fileNameSupplier) throws Throwable
	{
		final Step headerSetup = Step.HEADER_SETUP;

		if (LOG.isDebugEnabled())
		{
			LOG.debug("about to run method [{}] on target [{}]", pjp.getSignature().getName(), pjp.getTarget());
		}

		final String fileName = fileNameSupplier.get();
		final Date started = new Date();

		try
		{
			final Object proceed = pjp.proceed();
			monitorSuccessfulStep(headerSetup, started, "Successfully setup header for file [{}]. Header will be passed down pipeline.....", fileName);
			return proceed;
		}
		catch (final Exception e)
		{
			monitorFailedStep(headerSetup, started, e, "Failed to setup header for file [{}]", fileName);
			endMonitor(Status.FAILURE);
			throw new StepException(headerSetup, e);
		}
	}

	/**
	 * AOP Around implementation to monitor the init of a Header
	 * @param pjp    Pointcut to be executed
	 * @param header the Header being initialized
	 * @return the value from the execution
	 * @throws Throwable any exception thrown by the method called
	 */
	public Object aroundInit(final ProceedingJoinPoint pjp, final BatchHeader header) throws Throwable
	{
		return internalMonitor(pjp, header, Step.HEADER_INIT);
	}

	/**
	 * AOP Around implementation to monitor the transformation of a Header's file to an impex
	 * @param pjp    Pointcut to be executed
	 * @param header the Header being transformed
	 * @return the value from the execution
	 * @throws Throwable any exception thrown by the method called
	 */
	public Object aroundTransform(final ProceedingJoinPoint pjp, final BatchHeader header) throws Throwable
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("about to run method [{}] on target [{}]", pjp.getSignature().getName(), pjp.getTarget());
		}

		if (header instanceof ZipBatchHeader)
		{
			return aroundZipBatchHeaderTransform(pjp, (ZipBatchHeader) header);
		}
		return aroundBatchHeaderTransform(pjp, header);
	}

	private Object aroundBatchHeaderTransform(final ProceedingJoinPoint pjp, final BatchHeader header) throws Throwable
	{
		final Step headerTransformed = Step.HEADER_TRANSFORMED;

		final String fileName = header.getFile().getName();
		final Long sequenceId = header.getSequenceId();
		final Date started = new Date();
		try
		{
			final Object result = pjp.proceed();

			final BatchHeader resultingHeader = (BatchHeader) result;
			final List<File> transformedFiles = resultingHeader.getTransformedFiles();
			if (CollectionUtils.isNotEmpty(transformedFiles))
			{
				final List<String> transformedNames = transformedFiles.stream().map(File::getName).collect(Collectors.toList());
				monitorAction(ActionCodeGenerator.shortAlphaNumeric(), Status.SUCCESS, started, "Successfully transformed file [{}] " +
						"to impex(es) ({})", fileName, transformedNames);
			}

			monitorSuccessfulStep(headerTransformed, started, "Successfully transformed header for file [{}] with sequenceId [{}]", fileName, sequenceId);

			return result;
		}
		catch (final Exception e)
		{
			monitorFailedStep(headerTransformed, started, e, "Failed to transform header for file [{}] with sequenceId [{}]", fileName, sequenceId);
			endMonitor(Status.FAILURE);
			throw new StepException(headerTransformed, e);
		}
	}

	private Object aroundZipBatchHeaderTransform(final ProceedingJoinPoint pjp, final ZipBatchHeader header) throws Throwable
	{
		final Step headerTransformed = Step.HEADER_TRANSFORMED;

		final String zipFolderName = header.getOriginalFileName();
		final Long sequenceId = header.getSequenceId();
		final Date started = new Date();
		try
		{
			final Object result = pjp.proceed();

			final MultiMap originalToTransformed = ((ZipBatchHeader) result).getOriginalToTransformedMap();
			if (MapUtils.isNotEmpty(originalToTransformed))
			{
				//noinspection unchecked
				for (final String csvFile : (Set<String>) originalToTransformed.keySet())
				{
					monitorAction(ActionCodeGenerator.shortAlphaNumeric(), Status.SUCCESS, started, "For zip folder [{}], " +
							"successfully transformed expanded file [{}] to impex(es) ({})", zipFolderName, csvFile, originalToTransformed.get(csvFile));
				}
			}

			monitorSuccessfulStep(headerTransformed, started, "Successfully transformed header for zip folder [{}] " +
					"with sequenceId [{}]", zipFolderName, sequenceId);

			return result;
		}
		catch (final Exception e)
		{
			monitorFailedStep(headerTransformed, started, e, "Failed to transform header for zip folder [{}]", zipFolderName);
			endMonitor(Status.FAILURE);
			throw new StepException(headerTransformed, e);
		}

	}

	/**
	 * AOP Around implementation to monitor the execution of an transformed impex associated with a Header
	 *
	 * @param pjp    Pointcut to be executed
	 * @param header the Header being executed
	 * @return the value from the execution
	 * @throws Throwable any exception thrown by the method called
	 */
	public Object aroundExecute(final ProceedingJoinPoint pjp, final BatchHeader header) throws Throwable
	{
		return internalMonitor(pjp, header, Step.HEADER_EXECUTED);
	}

	/**
	 * AOP Around implementation to monitor the clean up of a Header
	 * @param pjp    Pointcut to be executed
	 * @param header the Header being cleaned up
	 * @return the value from the execution
	 * @throws Throwable any exception thrown by the method called
	 */
	public Object aroundCleanup(final ProceedingJoinPoint pjp, final BatchHeader header) throws Throwable
	{
		final Object result = internalMonitor(pjp, header, Step.HEADER_CLEANUP);
		endMonitor(Status.SUCCESS);
		return result;
	}

	protected Object internalMonitor(final ProceedingJoinPoint pjp, final BatchHeader header, final Step step) throws Throwable
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("about to run method [{}] on target [{}]", pjp.getSignature().getName(), pjp.getTarget());
		}

		final String fileName = getFileName(header);

		final Date started = new Date();
		try
		{
			final Object proceed = pjp.proceed();
			monitorSuccessfulStep(step, started, "Successfully processed file [{}]. " +
					"SequenceId [{}]", fileName, header.getSequenceId());
			return proceed;
		}
		catch (final Exception e)
		{
			monitorFailedStep(step, started, e, "Failed to process file [{}]", fileName);
			endMonitor(Status.FAILURE);
			throw new StepException(step, e);
		}
	}

	private String getFileName(final BatchHeader header)
	{
		return header instanceof ZipBatchHeader
				? ((ZipBatchHeader) header).getOriginalFileName()
				: header.getFile().getName();
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
}
