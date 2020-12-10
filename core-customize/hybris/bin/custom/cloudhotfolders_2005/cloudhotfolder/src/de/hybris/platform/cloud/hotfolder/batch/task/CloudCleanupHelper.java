/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.cloud.hotfolder.batch.task;

import de.hybris.platform.acceleratorservices.dataimport.batch.BatchHeader;
import de.hybris.platform.acceleratorservices.dataimport.batch.task.CleanupHelper;
import de.hybris.platform.cloud.hotfolder.spring.messaging.support.impl.UnmappedHotfolderChannel;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cleanup for the impex import process. Deletes all local files.
 */
public class CloudCleanupHelper extends CleanupHelper
{
	private static final Logger LOG = getLogger(UnmappedHotfolderChannel.class);

	/**
	 * Deletes the source file
	 *
	 * @param header - the header for the batch operation.
	 * @param error - true if an error occured during processing; otherwise false;
	 */
	@Override
	protected void cleanupSourceFile(final BatchHeader header, final boolean error)
	{
		if (header != null)
		{
			final File file = header.getFile();
			if(file != null && file.exists() && file.isFile())
			{
				try
				{
					LOG.debug("Cleaning up [{}].", file.getAbsolutePath());
					Files.delete(file.toPath());
				}
				catch (final IOException e)
				{
					LOG.error("Unable to clean up local file [{}].", file.getAbsolutePath(), e);
				}
			}
		}
	}

}
