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
import de.hybris.platform.cloud.hotfolder.spring.integration.zip.CloudZipHeaders;
import de.hybris.platform.util.CSVConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class ZipHeaderSetupTaskTest
{
	private static final String HEADER_KEY_FILE = FileHeaders.FILENAME;
	private static final String HEADER_KEY_UNZIPPED_FOLDER = CloudZipHeaders.UNZIP_DIRECTORY;
	private static final String UNZIPPED_FOLDER_NAME = "be24ef21-cc29-5d71-d7bd-93c35baa2d3b";

	private static final String ZIP_FILE = "rawFile.zip";
	private static final String ZIP_FOLDER = ZIP_FILE.substring(0, ZIP_FILE.length() - 4);

	private static final File UNZIPPED_ROOT_DIRECTORY = new File("unzippedDirectory");
	private static final File UNZIPPED_FOLDER_DIRECTORY = new File(UNZIPPED_ROOT_DIRECTORY, UNZIPPED_FOLDER_NAME);
	private static final File UNZIPPED_AS_DIRECTORY = new File(UNZIPPED_FOLDER_DIRECTORY, ZIP_FOLDER);

	private static final File ROOT_FILE = new File(UNZIPPED_AS_DIRECTORY, "root-data.impex");
	private static final File IMPORT_FOLDER_FILE = new File(UNZIPPED_AS_DIRECTORY, "import/folder-1.impex");
	private static final File CORE_DATA_FILE = new File(UNZIPPED_AS_DIRECTORY, "import/coredata/folder-2.impex");
	private static final File ESSENTIAL_DATA_FILE = new File(UNZIPPED_AS_DIRECTORY, "import/coredata/common/essential-data.impex");

	private static final String CATALOG = "catalog";
	private static final boolean NET = true;

	private ZipHeaderSetupTask task = new ZipHeaderSetupTask();

	@Mock
	private Message<Map> message;

	@Mock
	private MessageHeaders messageHeaders;

	private Map<String, File> unzippedFiles = new LinkedHashMap<>();

	@Before
	public void setUp() throws IOException
	{
		//task.setUnzippedRootDirectory(UNZIPPED_ROOT_DIRECTORY);
		task.setFileNameHeaderKey(HEADER_KEY_FILE);
		task.setUnZipDirectoryHeaderKey(HEADER_KEY_UNZIPPED_FOLDER);
		task.setCatalog(CATALOG);
		task.setNet(NET);

		given(message.getHeaders()).willReturn(messageHeaders);
		given(messageHeaders.get(HEADER_KEY_FILE)).willReturn(ZIP_FILE);
		given(messageHeaders.get(HEADER_KEY_UNZIPPED_FOLDER)).willReturn(UNZIPPED_FOLDER_DIRECTORY.getPath());

		given(message.getPayload()).willReturn(unzippedFiles);

		unzippedFiles.put("rootFile", ROOT_FILE);
		unzippedFiles.put("importFolderFile", IMPORT_FOLDER_FILE);
		unzippedFiles.put("coreDataFile", CORE_DATA_FILE);
		unzippedFiles.put("essentialDataFile", ESSENTIAL_DATA_FILE);

	}

	@Test
	public void shouldHandleUnzippedFilesBeingEmpty()
	{
		unzippedFiles.clear();

		task.execute(message);
	}

	@Test
	public void shouldReturnHeaderWithArgsSetAsExpected()
	{
		final ZipBatchHeader expected = new ZipBatchHeader();
		expected.setOriginalFileName(ZIP_FILE);
		expected.setFileUnzippedAs(UNZIPPED_AS_DIRECTORY);
		expected.setEncoding(CSVConstants.HYBRIS_ENCODING);
		expected.setCatalog(CATALOG);
		expected.setNet(NET);
		expected.setUnzippedFiles(Arrays.asList(ROOT_FILE, IMPORT_FOLDER_FILE, CORE_DATA_FILE, ESSENTIAL_DATA_FILE));

		final ZipBatchHeader result = task.execute(message);
		assertThat(result).isEqualToIgnoringGivenFields(expected, "unzippedFiles");
		assertThat(result.getUnzippedFiles()).containsExactly(ROOT_FILE, IMPORT_FOLDER_FILE, CORE_DATA_FILE, ESSENTIAL_DATA_FILE);
	}

}