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

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

/**
 * {@link AbstractRegexPatternFileListFilter} implementation which filters for {@link CloudBlob}
 * where file names match the given pattern
 *
 */
public class AzureBlobRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<CloudBlob>
{
	public AzureBlobRegexPatternFileListFilter(final String pattern)
	{
		super(pattern);
	}

	@Override
	protected String getFilename(final CloudBlob entry)
	{
		return AzureBlobFileInfo.getFilename(entry);
	}

	@Override
	protected boolean isDirectory(final CloudBlob file)
	{
		return AzureBlobFileInfo.isDirectory(file);
	}
}
