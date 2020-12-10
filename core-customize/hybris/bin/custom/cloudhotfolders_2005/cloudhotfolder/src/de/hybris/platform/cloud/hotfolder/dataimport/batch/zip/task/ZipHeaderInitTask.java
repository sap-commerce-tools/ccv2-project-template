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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.task;

import de.hybris.platform.acceleratorservices.dataimport.batch.util.SequenceIdParser;
import de.hybris.platform.acceleratorservices.util.RegexParser;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipHeaderTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Task to initialize the batch header with the sequence id and the language.
 */
public class ZipHeaderInitTask implements ZipHeaderTask
{
    private static final Logger LOG = getLogger(ZipHeaderInitTask.class);
    private static final long DEFAULT_SEQUENCE_ID = 0L;

    private SequenceIdParser sequenceIdParser;
    private RegexParser languageParser;
    private String fallbackLanguage;

    @Override
    public ZipBatchHeader executeZip(final ZipBatchHeader header)
    {
        header.setSequenceId(getSequenceId(new File(header.getOriginalFileName())));
        header.setLanguage(Optional.of(header.getOriginalFileName())
                .map(getLanguageParser()::parse)
                .orElse(getFallbackLanguage()));
        return header;
    }

    protected Long getSequenceId(final File file)
    {
        try
        {
            return getSequenceIdParser().getSequenceId(file);
        }
        catch (final IllegalArgumentException e)
        {
            LOG.warn("There was a problem getting the sequence id: " + e.getMessage());
            LOG.debug("Exception whilst getting the sequence id.",e);
            return DEFAULT_SEQUENCE_ID;
        }
    }

    protected SequenceIdParser getSequenceIdParser()
    {
        return sequenceIdParser;
    }

    @Required
    public void setSequenceIdParser(final SequenceIdParser sequenceIdParser)
    {
        this.sequenceIdParser = sequenceIdParser;
    }

    protected RegexParser getLanguageParser()
    {
        return languageParser;
    }

    @Required
    public void setLanguageParser(final RegexParser languageParser)
    {
        this.languageParser = languageParser;
    }

    protected String getFallbackLanguage()
    {
        return fallbackLanguage;
    }

    @Required
    public void setFallbackLanguage(final String fallbackLanguage)
    {
        this.fallbackLanguage = fallbackLanguage;
    }
}
