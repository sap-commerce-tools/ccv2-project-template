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
package de.hybris.platform.cloud.azure.hotfolder.aop;

import com.microsoft.azure.storage.blob.CloudBlob;

import de.hybris.platform.cloud.azure.hotfolder.remote.session.AzureBlobFileInfo;
import de.hybris.platform.cloud.azure.hotfolder.remote.session.ExtendedSession;
import de.hybris.platform.cloud.commons.aop.exception.StepException;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;
import de.hybris.platform.cloud.hotfolder.aop.AbstractMonitoringAspect;
import org.apache.commons.io.FileUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * AOP implementation to monitor the transfer of remote blobs to local disk
 *
 */
// Suppress Sonar warnings - AOP aspects are supposed to throw Throwables.
@SuppressWarnings({"squid:S00112"})
public class BlobTransferredAspect extends AbstractMonitoringAspect
{
	private static final Logger LOG = getLogger(BlobTransferredAspect.class);

	/**
	 * AOP Around implementation to capture the size of a file and the time taken to transfer it
	 *
	 * @param pjp  Pointcut to be executed
	 * @param path the path being requested
	 * @return the value from the execution
	 * @throws Throwable any exception thrown by the method called
	 */
	public Object aroundRead(final ProceedingJoinPoint pjp, final String path) throws Throwable
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("about to run method [{}] on target [{}] with path [{}]", pjp.getSignature().getName(), pjp.getTarget(),
					path);
		}

		final CloudBlob blob = getTargetOfMonitoringAspect(pjp, path);
		final String fileName = AzureBlobFileInfo.getFilename(blob);
		final long modified = AzureBlobFileInfo.getModified(blob);
		final long size = AzureBlobFileInfo.getSize(blob);
		beginMonitor(SystemArea.HOT_FOLDER, fileName + modified);

		final Date started = new Date();
		try
		{
			final Object result = pjp.proceed();
			monitorSuccessfulStep(Step.DOWNLOADED, started, "Successfully read blob [{}] of size [{}]", path,
					FileUtils.byteCountToDisplaySize(size));
			checkpointMonitor();
			return result;
		}
		catch (final Exception e)
		{
			monitorFailedStep(Step.DOWNLOADED, started, e, "Failed to read blob [{}]", path);
			endMonitor(Status.FAILURE);
			throw new StepException(Step.DOWNLOADED, e);
		}

	}

	private CloudBlob getTargetOfMonitoringAspect(final ProceedingJoinPoint pjp, final String path) throws IOException
	{
		Assert.isTrue(pjp.getTarget() instanceof ExtendedSession, "pjp is not of type Extended Session");

		@SuppressWarnings("unchecked")
		final ExtendedSession<CloudBlob> session = (ExtendedSession<CloudBlob>) pjp.getTarget();
		return session.get(path);
	}
}
