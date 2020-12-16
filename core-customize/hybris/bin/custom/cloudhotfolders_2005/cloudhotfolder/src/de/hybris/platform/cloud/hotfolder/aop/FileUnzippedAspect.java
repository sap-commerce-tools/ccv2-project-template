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

import de.hybris.platform.cloud.commons.aop.exception.StepException;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * AOP implementation to monitor the exploding of a ZIP file
 */
// Suppress Sonar warnings - AOP aspects are supposed to throw Throwables.
@SuppressWarnings({"squid:S00112"})
public class FileUnzippedAspect extends AbstractMonitoringAspect
{
	private static final Logger LOG = getLogger(FileUnzippedAspect.class);

	private String fileNameHeaderKey;
	private String fileLastModifiedHeaderKey;

	/**
	 * AOP Around implementation to capture the time taken to explode a ZIP file
	 *
	 * @param pjp     Pointcut to be executed
	 * @param message the message being passed by Spring Integration
	 * @return the value from the execution
	 * @throws Throwable any exception/error thrown during set up of the aspect for logging or a {@link StepException} in the
	 *                   case of an exception during execution of the method call
	 */
	public Object aroundUnzipped(final ProceedingJoinPoint pjp, final Message<?> message) throws Throwable
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("about to run method [{}] on target [{}]", pjp.getSignature().getName(), pjp.getTarget());
		}

		final MessageHeaders headers = message.getHeaders();
		final String unzippedTo = headers.getId().toString();
		final String fileName = (String) headers.get(getFileNameHeaderKey());
		final long modified = (long) headers.get(getFileLastModifiedHeaderKey());

		resumeMonitor(SystemArea.HOT_FOLDER, fileName + modified);

		final Date started = new Date();
		try
		{
			final Object proceed = pjp.proceed();
			monitorSuccessfulStep(Step.FILE_UNZIPPED, started, "Successfully unzipped file [{}] to [{}]", fileName, unzippedTo);
			return proceed;
		}
		catch (final Exception e)
		{
			monitorFailedStep(Step.FILE_UNZIPPED, started, e, "Failed to unzip file [{}]", fileName);
			endMonitor(Status.FAILURE);
			throw new StepException(Step.FILE_UNZIPPED, e);
		}

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
}
