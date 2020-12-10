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

import de.hybris.platform.commerceservices.setup.impl.DefaultSetupImpexService;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.impex.ImportConfig;
import de.hybris.platform.servicelayer.impex.ImportResult;
import de.hybris.platform.servicelayer.impex.impl.FileBasedImpExResource;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @inheritDoc Extended implementation of {@link DefaultSetupImpexService} which retrieves an impex
 * {@link File} at a given location
 */
public class FileBasedSetupImpexService extends DefaultSetupImpexService
{
    private static final Logger LOG = getLogger(FileBasedSetupImpexService.class);

    @Override
    public void importImpexFile(final String file, final boolean errorIfMissing)
    {
        importImpexFile(file, errorIfMissing, false);
    }

    @Override
    public void importImpexFile(final String file, final boolean errorIfMissing, final boolean legacyMode)
    {
        // Handle starting with additional '/' from CoreDataImportService/SampleDataImportService
        final String adjustedPath = Optional.ofNullable(file)
                .map(s -> s.substring(1))
                .orElse("");

        final File impexFile = new File(adjustedPath);
        if (!impexFile.exists() || !impexFile.isFile())
        {
            LOG.error("Importing [" + adjustedPath
                    + (errorIfMissing ? "]... ERROR (MISSING FILE)" : "]... SKIPPED (Optional File Not Found)"));
        }
        else
        {
            importImpexFile(adjustedPath, impexFile, legacyMode);

            // Try to import language specific impex files
            if (adjustedPath.endsWith(getImpexExt()))
            {
                final String filePath = adjustedPath.substring(0, adjustedPath.length() - getImpexExt().length());
                final List<LanguageModel> languages = getCommonI18NService().getAllLanguages();
                importForLanguages(legacyMode, filePath, languages);
            }
        }
    }

    @Override
    protected void importImpexFile(final String file, final InputStream stream, final boolean legacyMode)
    {
        throw new UnsupportedOperationException("Please use method taking String, File, boolean instead");
    }

    protected void importImpexFile(final String file, final File impexFile, final boolean legacyMode)
    {
        final String message = "Importing [" + file + "]...";

        try
        {
            LOG.info(message);

            final ImportConfig importConfig = new ImportConfig();
            importConfig.setScript(new FileBasedImpExResource(impexFile, getFileEncoding()));
            importConfig.setLegacyMode(legacyMode);

            final ImportResult importResult = getImportService().importData(importConfig);
            if (importResult.isError())
            {
                LOG.error(message + " FAILED");
            }
        }
        catch (final Exception e)
        {
            LOG.error(message + " FAILED", e);
        }
    }

    protected void importForLanguages(final boolean legacyMode, final String filePath, final List<LanguageModel> languages)
    {
        for (final LanguageModel language : languages)
        {
            final String languageFilePath = filePath + "_" + language.getIsocode() + getImpexExt();
            final File langFile = new File(languageFilePath);
            if (langFile.exists() && langFile.isFile())
            {
                importImpexFile(languageFilePath, langFile, legacyMode);
            }
        }
    }
}
