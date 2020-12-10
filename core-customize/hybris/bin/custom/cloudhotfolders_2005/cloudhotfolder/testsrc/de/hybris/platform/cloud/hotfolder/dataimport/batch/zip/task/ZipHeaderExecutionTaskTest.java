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

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.service.UnzippedFolderImportService;

import java.io.File;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class ZipHeaderExecutionTaskTest
{
	private static final String UNZIPPED_FOLDER_NAME = "be24ef21-cc29-5d71-d7bd-93c35baa2d3b";
	private static final File UNZIPPED_DIRECTORY = new File("workDirectory", UNZIPPED_FOLDER_NAME);
	private static final File UNZIPPED_AS = new File(UNZIPPED_DIRECTORY, "original-file");

	private ZipHeaderExecutionTask task = new ZipHeaderExecutionTask();

	@Mock
	private UnzippedFolderImportService coreDataImportService;

	@Mock
	private UnzippedFolderImportService sampleDataImportService;

	private ZipBatchHeader header = new ZipBatchHeader();

	@Before
	public void setUp()
	{
		task.setUnzippedFolderImportServices(ImmutableMap.of(Pattern.compile("^coredata.*"), coreDataImportService, Pattern.compile("^sampledata.*"), sampleDataImportService));

		header.setFileUnzippedAs(UNZIPPED_AS);
		header.setOriginalFileName("original-file.zip");
	}

	@Test
	public void shouldReturnGivenHeader()
	{
		assertThat(task.executeZip(header)).isEqualTo(header);
	}

	@Test
	public void shouldTryToImportCoreDataWhenFolderFoundWithinWorkingFolder()
	{
		header.setOriginalFileName("coredata-00.zip");

		task.executeZip(header);

		verify(coreDataImportService).execute(header);
		verify(sampleDataImportService, never()).execute(header);
	}

	@Test
	public void shouldTryToImportSampleDataWhenFolderFoundWithinWorkingFolder()
	{
		header.setOriginalFileName("sampledata-00.zip");

		task.executeZip(header);

		verify(coreDataImportService, never()).execute(header);
		verify(sampleDataImportService).execute(header);
	}

}