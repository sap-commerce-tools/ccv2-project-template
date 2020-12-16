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

import de.hybris.platform.acceleratorservices.dataimport.batch.BatchHeader;
import de.hybris.platform.acceleratorservices.dataimport.batch.converter.ImpexConverter;
import de.hybris.platform.acceleratorservices.dataimport.batch.task.ImpexTransformerTask;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipHeaderTask;
import org.springframework.util.Assert;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transformer that transforms all CSV files within the exploded ZIP into impex files.
 */
public class ZipHeaderTransformerTask extends ImpexTransformerTask implements ZipHeaderTask
{
	private static final String CSV_SUFFIX = "csv";
	private static final String IMPEX_SUFFIX = "impex";

	/**
	 * Default implementation with {@link BatchHeader} to delegate onto {@link #executeZip}
	 * @throws IllegalArgumentException when header is not of type {@link ZipBatchHeader}
	 */
	@Override
	public BatchHeader execute(final BatchHeader header)
	{
		Assert.isInstanceOf(ZipBatchHeader.class, header);
		return executeZip((ZipBatchHeader) header);
	}

	@Override
	public ZipBatchHeader executeZip(final ZipBatchHeader header)
	{
		header.getUnzippedFiles().stream()
				.filter(f -> f.getName().endsWith(CSV_SUFFIX))
				.forEach(f -> {
					try
					{
						convertCsv(header, f);
					}
					catch (final IOException e)
					{
						throw new UncheckedIOException(e);
					}
				});

		return header;
	}

	protected void convertCsv(final ZipBatchHeader header, final File file) throws IOException
	{
		final List<ImpexConverter> converters = getConverters(file);
		int position = 1;
		for (final ImpexConverter converter : converters)
		{
			final File impexFile = getImpexFile(file, position++);
			if (convertFile(header, file, impexFile, converter))
			{
				header.addTransformedFile(impexFile);
				header.addOriginalToTransformedEntry(file.getName(), impexFile.getName());
			}
			else
			{
				getCleanupHelper().cleanupFile(impexFile);
			}
		}
	}

	@Override
	protected File getImpexFile(final File file, final int position)
	{
		return new File(file.getParent(), file.getName().replace("." + CSV_SUFFIX, "-" + position + "." + IMPEX_SUFFIX));
	}

	@Override
	protected void buildReplacementSymbols(final Map<String, String> symbols, final BatchHeader header,
	                                       final ImpexConverter converter)
	{
		super.buildReplacementSymbols(symbols, header, converter);
		final ZipBatchHeader zipBatchHeader = (ZipBatchHeader) header;
		final String absolutePathToUnzippedFolder = Optional.ofNullable(zipBatchHeader.getFileUnzippedAs())
				.map(File::getAbsolutePath)
				.orElseThrow(() -> new IllegalArgumentException("'header.fileUnzippedAs' cannot be null"));

		//For an exploded zip the $SOURCE_FILE$ parameter is actually the $SOURCE_DIR$, so replace
		symbols.put("$SOURCE_DIR$", absolutePathToUnzippedFolder);
		symbols.put("$SOURCE_FILE$", absolutePathToUnzippedFolder);
		//Also set the $BASE_SOURCE_DIR$ as the unzipped folder
		symbols.put("$BASE_SOURCE_DIR$", absolutePathToUnzippedFolder);
	}

	// Suppress Sonar warnings - Overridden to make public for testing.
	@Override
	@SuppressWarnings("squid:S1185")
	public void setConverterMap(final Map<String, List<ImpexConverter>> converterMap)
	{
		super.setConverterMap(converterMap);
	}

	// Suppress Sonar warnings - Overridden to make public for testing.
	@Override
	@SuppressWarnings("squid:S1185")
	public boolean convertFile(final BatchHeader header, final File file, final File impexFile, final ImpexConverter converter)
			throws UnsupportedEncodingException, FileNotFoundException
	{
		return super.convertFile(header, file, impexFile, converter);
	}

}
