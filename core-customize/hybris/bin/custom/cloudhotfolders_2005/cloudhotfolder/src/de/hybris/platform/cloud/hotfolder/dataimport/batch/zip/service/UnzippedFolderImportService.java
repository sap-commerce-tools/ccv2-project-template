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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.service;

import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;

/**
 * Strategy for processing a {@link ZipBatchHeader} representing an exploded ZIP file
 */
public interface UnzippedFolderImportService
{
	/**
	 * Process the {@link ZipBatchHeader}
	 */
	void execute(final ZipBatchHeader header);
}
