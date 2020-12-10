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

import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.util.CSVConstants;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Map;

/**
 * Initially setup the batch header. The header is used throughout the pipeline as a reference and for cleanup.
 */
public class ZipHeaderSetupTask
{
	private String fileNameHeaderKey;
	private String unZipDirectoryHeaderKey;
	private String catalog;
	private boolean net;

	public ZipBatchHeader execute(final Message<?> message)
	{
		Assert.isTrue(message.getPayload() instanceof Map, "message.payload to be of type Map.class");
		@SuppressWarnings("unchecked") final Map<Object, File> unzippedFiles = (Map) message.getPayload();

		final String fileName = (String) message.getHeaders().get(fileNameHeaderKey);
		Assert.notNull(fileName, "message.headers should contain '" + fileNameHeaderKey + "'");
		final String explodedFolderName = fileName.substring(0, fileName.length() - 4);

		final String unzippedFolderName = message.getHeaders().get(unZipDirectoryHeaderKey).toString();
		Assert.notNull(unzippedFolderName, "message.headers should contain '" + unZipDirectoryHeaderKey + "'");
		final File unzippedAs = new File(unzippedFolderName, explodedFolderName);

		final ZipBatchHeader header = new ZipBatchHeader();
		header.setOriginalFileName(fileName);
		header.setFileUnzippedAs(unzippedAs);
		header.setUnzippedFiles(unzippedFiles.values());
		header.setEncoding(CSVConstants.HYBRIS_ENCODING);
		header.setCatalog(catalog);
		header.setNet(net);
		return header;
	}

	/**
	 * Set the header key to use to get a reference to the original file
	 */
	@Required
	public void setFileNameHeaderKey(final String fileNameHeaderKey)
	{
		this.fileNameHeaderKey = fileNameHeaderKey;
	}

	/**
	 * Set the header key to use to get the name of the folder the file was extracted into
	 */
	@Required
	public void setUnZipDirectoryHeaderKey(final String unZipDirectoryHeaderKey)
	{
		this.unZipDirectoryHeaderKey = unZipDirectoryHeaderKey;
	}

	@Required
	public void setCatalog(final String catalog)
	{
		this.catalog = catalog;
	}

	@Required
	public void setNet(final boolean net)
	{
		this.net = net;
	}

}
