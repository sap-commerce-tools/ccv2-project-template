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

import com.google.common.collect.ImmutableMap;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorservices.dataimport.batch.converter.ImpexConverter;
import de.hybris.platform.acceleratorservices.dataimport.batch.task.CleanupHelper;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class ZipHeaderTransformerTaskTest
{
	@Spy
	private final ZipHeaderTransformerTask task = new ZipHeaderTransformerTask();

	@Mock
	private ImpexConverter productImpexConverter;

	@Mock
	private CleanupHelper cleanupHelper;

	private final ZipBatchHeader header = new ZipBatchHeader();

	private final File impexFile = new File("raw.impex");
	private final File csvFile = new File("folder", "product.csv");
	private final File convertedImpexFile = new File("folder", "product-1.impex");

	@Before
	public void setUp() throws IOException
	{
		task.setConverterMap(ImmutableMap.of("product", Collections.singletonList(productImpexConverter)));
		task.setCleanupHelper(cleanupHelper);

		header.setUnzippedFiles(Arrays.asList(impexFile, csvFile));
		header.setTransformedFiles(new ArrayList<>());

		givenThatFileConversionResultedIn(true);
	}

	private void givenThatFileConversionResultedIn(final boolean result) throws IOException
	{
		willReturn(result)
				.given(task)
				.convertFile(header, csvFile, convertedImpexFile, productImpexConverter);
	}

	@Test
	public void csvConvertedSuccessfullyShouldBeAddedToTransformedFiles()
	{
		task.executeZip(header);

		assertThat(header.getTransformedFiles()).containsExactly(convertedImpexFile);
		verify(cleanupHelper, never()).cleanupFile(convertedImpexFile);
	}

	@Test
	public void csvNotConvertedSuccessfullyShouldBeCleanedUp() throws IOException
	{
		givenThatFileConversionResultedIn(false);

		task.executeZip(header);

		assertThat(header.getTransformedFiles()).isEmpty();
		verify(cleanupHelper).cleanupFile(convertedImpexFile);
	}

	@Test
	public void buildReplacementSymbolsShouldAdjustStandardVariables()
	{
		final File rootDir = new File("parentFolder");
		final File subDir = new File(rootDir, "childFolder");

		header.setCatalog("catalog");
		header.setLanguage("language");
		header.setNet(true);
		header.setStoreBaseDirectory("storeBaseDir");
		header.setFileUnzippedAs(subDir);

		final Map<String, String> symbols = new LinkedHashMap<>();
		task.buildReplacementSymbols(symbols, header, productImpexConverter);

		final Map<String, String> expected = new LinkedHashMap<>();
		expected.put("$CATALOG$", "catalog");
		expected.put("$LANGUAGE$", "language");
		expected.put("$NET$", "true");
		expected.put("$BASE_SOURCE_DIR$", subDir.getAbsolutePath());
		expected.put("$SOURCE_FILE$", subDir.getAbsolutePath());
		expected.put("$SOURCE_DIR$", subDir.getAbsolutePath());

		assertThat(symbols).containsAllEntriesOf(expected);
	}


}