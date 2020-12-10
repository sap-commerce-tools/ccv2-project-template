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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip;

import de.hybris.platform.acceleratorservices.dataimport.batch.BatchHeader;

import org.springframework.util.Assert;

import java.io.File;
import java.util.Collection;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

/**
 * @inheritDoc It also includes
 * <ul>
 * <li>originalFileName: the original ZIP file name before it was exploded</li>
 * <li>unzippedFolder: the directory into which the ZIP was exploded</li>
 * <li>unzippedFiles: a collection of files that were exploded from within the ZIP</li>
 * <li>originalToTransformedMap: a collection which maps an original file name to the resulting impex file(s)
 * </ul>
 */
public class ZipBatchHeader extends BatchHeader
{
	private String originalFileName;
	private File unzippedFolder;
	private Collection<File> unzippedFiles;
	private MultiMap originalToTransformedMap;

	public String getOriginalFileName()
	{
		return originalFileName;
	}

	public void setOriginalFileName(final String originalFileName)
	{
		this.originalFileName = originalFileName;
	}

	/**
	 * Get where the file was actually unzipped to
	 * <p>Note: spring-integration-zip creates a UUID folder (Message.ID) and then unzips the file within</p>
	 * e.g. file_name.zip -> be24ef21-cc29-5d71-d7bd-93c35baa2d3b/file_name
	 *
	 * @return - A {@link File} pointing to where the archive was unzipped
	 */
	public File getFileUnzippedAs()
	{
		return getFile();
	}

	/**
	 * Set where the file was actually unzipped to
	 * <p>spring-integration-zip creates a UUID folder (Message.ID) and then unzips the file within</p>
	 * e.g. file_name.zip -> be24ef21-cc29-5d71-d7bd-93c35baa2d3b/file_name
	 * <p>Note: also sets <code>unzippedFolder</code> to the parent of the given directory</p>
	 *
	 * @throws IllegalArgumentException where given directory has no parent
	 */
	public void setFileUnzippedAs(final File dir)
	{
		Assert.notNull(dir.getParentFile(), "parentFile of given folder cannot be null");
		setFile(dir);
		this.unzippedFolder = dir.getParentFile();
	}

	/**
	 * Get the directory encapsulating the unzipped file
	 *
	 * @see #setFileUnzippedAs
	 */
	public File getUnzippedFolder()
	{
		return unzippedFolder;
	}

	/**
	 * Get the collection files resulting from unzipping the original file
	 */
	public Collection<File> getUnzippedFiles()
	{
		return unzippedFiles;
	}

	/**
	 * Set the collection files resulting from unzipping the original file
	 *
	 * @param unzippedFiles - The unzipped files
	 */
	public void setUnzippedFiles(final Collection<File> unzippedFiles)
	{
		this.unzippedFiles = unzippedFiles;
	}

	/**
	 * Get the collection containing the mappings of original csv files to their resulting impex file(s)
	 *
	 * @return
	 */
	public MultiMap getOriginalToTransformedMap()
	{
		return originalToTransformedMap;
	}

	/**
	 * Set the multimap which maps csv files name to their resulting impex
	 *
	 * @param originalToTransformedMap
	 */
	public void setOriginalToTransformedMap(final MultiMap originalToTransformedMap)
	{
		this.originalToTransformedMap = originalToTransformedMap;
	}

	/**
	 * Adds a mapping of csv file name to impex file name
	 *
	 * @param original    the original filename
	 * @param transformed the resulting transformed filename
	 */
	public void addOriginalToTransformedEntry(final String original, final String transformed)
	{
		if (originalToTransformedMap == null)
		{
			originalToTransformedMap = new MultiValueMap();
		}
		originalToTransformedMap.put(original, transformed);
	}
}
