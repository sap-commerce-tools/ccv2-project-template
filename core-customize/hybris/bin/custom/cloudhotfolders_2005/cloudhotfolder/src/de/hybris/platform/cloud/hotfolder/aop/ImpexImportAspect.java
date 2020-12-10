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

import de.hybris.platform.cloud.commons.aop.exception.ActionException;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.hotfolder.aop.utils.ActionCodeGenerator;
import de.hybris.platform.impex.model.ImpExMediaModel;
import de.hybris.platform.servicelayer.impex.ImpExResource;
import de.hybris.platform.servicelayer.impex.ImportConfig;
import de.hybris.platform.servicelayer.impex.ImportResult;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * AOP implementation to monitor the set up and subsequent execution of an Impex by the Hybris Platform
 */
// Suppress Sonar warnings - AOP aspects are supposed to throw Throwables.
@SuppressWarnings({"squid:S00112"})
public class ImpexImportAspect extends AbstractMonitoringAspect
{
	private static final Logger LOG = getLogger(ImpexImportAspect.class);
	private static final String OPTIONAL = "OPTIONAL";
	private static final String MANDATORY = "MANDATORY";

	/**
	 * AOP Around implementation to capture the name of the impex we're about to import, as we lose this information
	 * at the actual point of impex import
	 *
	 * @param pjp  Pointcut to be executed
	 * @param file The file path passed to the set up service
	 * @throws Throwable any exception/error thrown during set up of the aspect for logging or a {@link ActionException} in the
	 *                   case of an exception during execution of the method call
	 */
	public Object aroundSetupImpex(final ProceedingJoinPoint pjp, final String file, final boolean errorIfMissing) throws Throwable
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("about to run method [{}] on target [{}]", pjp.getSignature().getName(), pjp.getTarget());
		}

		final Date started = new Date();
		try
		{
			monitorAction(ActionCodeGenerator.shortAlphaNumeric(), Status.IN_PROGRESS, started,
					"Attempting to process [{}] impex file [{}]...", errorIfMissing ? MANDATORY : OPTIONAL, getFileName(file));

			return pjp.proceed();
		}
		catch (final Exception e)
		{
			// Failure/success monitoring of actual impex import is captured in aroundImportData
			// We're only capturing the filename here so it's unnecessary to add monitoring on an exception
			if (LOG.isDebugEnabled())
			{
				LOG.debug("Exception occurred just prior to impex import. Possibly during impex set up...", e);
			}
			throw new ActionException("Set up impex prior to import", Step.HEADER_EXECUTED, e);
		}
	}

	private String getFileName(final String absPath)
	{
		return new File(absPath).getName();
	}

	/**
	 * AOP Around implementation to capture the size of a file and the time taken to transfer it
	 *
	 * @param pjp    Pointcut to be executed
	 * @param config the ImportConfig for the impex
	 * @return the value from the execution
	 * @throws Throwable any exception/error thrown during set up of the aspect for logging or a {@link ActionException} in the
	 *                   case of an exception during execution of the method call
	 */
	public Object aroundImportData(final ProceedingJoinPoint pjp, final ImportConfig config) throws Throwable
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("about to run method [{}] on target [{}]", pjp.getSignature().getName(), pjp.getTarget());
		}

		final Optional<ImpExMediaModel> media = Optional.of(config)
				.map(ImportConfig::getScript)
				.map(ImpExResource::getMedia);

		final String mediaCode = media
				.map(ImpExMediaModel::getCode)
				.orElse(ActionCodeGenerator.randomUUID());

		final Date started = new Date();
		try
		{
			final ImportResult result = (ImportResult) pjp.proceed();
			if (result.isRunning())
			{
				monitorAction(mediaCode, Status.IN_PROGRESS, started, "Impex is still running, will not wait.  Maybe consider setting config.synchronous to true");
			}
			else if (result.isSuccessful())
			{
				monitorAction(mediaCode, Status.SUCCESS, started, "Impex ran successfully");
			}
			else if (result.isError())
			{
				final String supportingInfo = Optional.of(result)
						.filter(ImportResult::hasUnresolvedLines)
						.map(ImportResult::getUnresolvedLines)
						.map(ImpExMediaModel::getPreview)
						.orElse("No supporting info, please see logs");
				monitorAction(mediaCode, Status.FAILURE, started, "Impex didn't run successfully. {}", supportingInfo);
			}

			return result;
		}
		catch (final Exception e)
		{
			monitorAction(mediaCode, Status.FAILURE, started, "Impex didn't run successfully and exception was thrown. Ex Message: {}", e.getMessage());
			throw new ActionException("Impex import", Step.HEADER_EXECUTED, e);
		}
	}

}
