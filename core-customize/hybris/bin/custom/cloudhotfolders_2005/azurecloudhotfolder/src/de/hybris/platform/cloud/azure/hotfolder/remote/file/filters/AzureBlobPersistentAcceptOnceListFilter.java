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
package de.hybris.platform.cloud.azure.hotfolder.remote.file.filters;

import com.microsoft.azure.storage.blob.CloudBlob;

import de.hybris.platform.cloud.azure.hotfolder.remote.session.AzureBlobFileInfo;

import org.slf4j.Logger;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link AbstractPersistentAcceptOnceFileListFilter} implementation which filters for {@link CloudBlob}
 * which are files and have not not already been accepted
 *
 */
public class AzureBlobPersistentAcceptOnceListFilter extends AbstractPersistentAcceptOnceFileListFilter<CloudBlob>
{
	private static final Logger LOG = getLogger(AzureBlobPersistentAcceptOnceListFilter.class);

	public AzureBlobPersistentAcceptOnceListFilter(final ConcurrentMetadataStore store, final String prefix)
	{
		super(store, prefix);
	}

	@Override
	public boolean accept(final CloudBlob file)
	{
		try
		{
			return file != null && super.accept(file);
		}
		catch (final RuntimeException ex)
		{
			LOG.error("Exception whilst filtering for duplicate file.", ex);
			return false;
		}
	}

	@Override
	protected long modified(final CloudBlob entry)
	{
		return AzureBlobFileInfo.getModified(entry);
	}

	@Override
	protected String fileName(final CloudBlob entry)
	{
		return AzureBlobFileInfo.getRemotePath(entry);
	}

}
