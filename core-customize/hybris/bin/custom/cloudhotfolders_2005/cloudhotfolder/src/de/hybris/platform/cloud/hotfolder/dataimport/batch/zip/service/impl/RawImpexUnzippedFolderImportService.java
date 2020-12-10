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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.service.impl;

import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.service.UnzippedFolderImportService;
import de.hybris.platform.commerceservices.setup.SetupImpexService;

import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**)
 * @inheritDoc Imports any Impex files contained, or converted from files, within the exploded directory
 */
public class RawImpexUnzippedFolderImportService implements UnzippedFolderImportService
{
	private static final String IMPEX_SUFFIX = "impex";
	private SetupImpexService setupImpexService;

	/**
	 * @inheritDoc
	 */
	@Override
	public void execute(final ZipBatchHeader header)
	{
		final List<File> impexFiles = getImpexFiles(header.getUnzippedFiles(), header.getTransformedFiles());
		for (final File impex : impexFiles)
		{
			//Add additional "/" to start of path, so can be removed, mimics CoreDataImportService/SampleDataImportService
			getSetupImpexService().importImpexFile("/" + impex.getAbsolutePath(), true);
		}
	}

	protected List<File> getImpexFiles(final Collection<File> unzippedFiles, final Collection<File> transformedFiles)
	{
		return Stream.of(unzippedFiles, transformedFiles)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.filter(f -> f.getName().endsWith(IMPEX_SUFFIX))
				.sorted()
				.collect(Collectors.toList());
	}

	protected SetupImpexService getSetupImpexService()
	{
		return setupImpexService;
	}

	@Required
	public void setSetupImpexService(final SetupImpexService setupImpexService)
	{
		this.setupImpexService = setupImpexService;
	}

}
