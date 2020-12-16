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
import de.hybris.platform.commerceservices.dataimport.AbstractDataImportService;
import de.hybris.platform.commerceservices.setup.AbstractSystemSetup;
import de.hybris.platform.commerceservices.setup.data.ImportData;
import de.hybris.platform.core.initialization.SystemSetup;
import de.hybris.platform.core.initialization.SystemSetupContext;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @inheritDoc
 * Checks if the unzipped directory contains a required directory before executing the contents of the header
 */
public class DataImportUnzippedFolderImportService implements UnzippedFolderImportService, BeanNameAware
{
	private static final Logger LOG = getLogger(DataImportUnzippedFolderImportService.class);

	private AbstractDataImportService abstractDataImportService;
	private AbstractSystemSetup abstractSystemSetup;
	private List<ImportData> importDatas;
	private String requiredSubdirectory;
	private String beanName;

	/**
	 * @inheritDoc
	 */
	@Override
	public void execute(final ZipBatchHeader header)
	{
		final File unzippedAs = header.getFileUnzippedAs();
		final SystemSetupContext context = createContext(unzippedAs.getAbsolutePath());
		final AbstractDataImportService importService = getAbstractDataImportService();
		if (requiredDirExists(unzippedAs, this.getRequiredSubdirectory()))
		{
			LOG.info("Will try to import files within [{}] using importService [{}]", unzippedAs, importService);
			importService.execute(getAbstractSystemSetup(), context, getImportDatas());
		}
		else
		{
			LOG.info("directory [{}] didn't contain the required subfolder [{}] for importService [{}]", unzippedAs, this.getRequiredSubdirectory(), importService);
		}
	}

	protected SystemSetupContext createContext(final String path)
	{
		return new SystemSetupContext(Collections.emptyMap(), SystemSetup.Type.NOTDEFINED, SystemSetup.Process.NOTDEFINED, path);
	}

	protected boolean requiredDirExists(final File folderToProcess, final String requiredDir)
	{
		return new File(folderToProcess, requiredDir).exists();
	}

	protected AbstractDataImportService getAbstractDataImportService()
	{
		return abstractDataImportService;
	}

	@Required
	public void setAbstractDataImportService(final AbstractDataImportService abstractDataImportService)
	{
		this.abstractDataImportService = abstractDataImportService;
	}

	protected AbstractSystemSetup getAbstractSystemSetup()
	{
		return abstractSystemSetup;
	}

	@Required
	public void setAbstractSystemSetup(final AbstractSystemSetup abstractSystemSetup)
	{
		this.abstractSystemSetup = abstractSystemSetup;
	}

	protected List<ImportData> getImportDatas()
	{
		return importDatas;
	}

	@Required
	public void setImportDatas(final List<ImportData> importDatas)
	{
		this.importDatas = importDatas;
	}

	protected String getRequiredSubdirectory()
	{
		return requiredSubdirectory;
	}

	@Required
	public void setRequiredSubdirectory(final String requiredSubdirectory)
	{
		this.requiredSubdirectory = requiredSubdirectory;
	}

	@Override
	public void setBeanName(final String beanName)
	{
		this.beanName = beanName;
	}

	@Override
	public String toString()
	{
		return this.beanName;
	}
}
