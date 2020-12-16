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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.service.impl;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;
import de.hybris.platform.commerceservices.setup.SetupImpexService;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.inOrder;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class RawImpexUnzippedFolderImportServiceTest
{
	private RawImpexUnzippedFolderImportService service = new RawImpexUnzippedFolderImportService();

	@Mock
	private SetupImpexService setupImpexService;

	private File unzippedFolder = new File(new File("root"), "unzippedFolderName");
	private File unzippedAs = new File(unzippedFolder, "file_unzipped_as_folder");

	private File rootFile1 = new File(unzippedAs, "_file.csv");
	private File rootFile1Transformed = new File(unzippedAs, "_file.impex");
	private File rootFile2 = new File(unzippedAs, "z-file.impex");

	private File rootFolder1 = new File(unzippedAs, "a-folder");
	private File rootFolder1File1 = new File(rootFolder1, "_file.impex");
	private File rootFolder1File2 = new File(rootFolder1, "z-file.impex");
	private File rootFolder1Subfolder = new File(rootFolder1, "b-folder");
	private File rootFolder1SubfolderFile1 = new File(rootFolder1Subfolder, "file.impex");

	private File rootFolder2 = new File(unzippedAs, "b-folder");
	private File rootFolder2File1 = new File(rootFolder2, "file.impex");
	private File rootFolder2Subfolder = new File(rootFolder2, "c-folder");
	private File rootFolder2SubfolderFile1 = new File(rootFolder2Subfolder, "file.impex");

	private ZipBatchHeader header = new ZipBatchHeader();

	@Before
	public void setUp()
	{
		service.setSetupImpexService(setupImpexService);

		header.setFileUnzippedAs(unzippedAs);
		header.setUnzippedFiles(Arrays.asList(rootFile2, rootFolder1File2, rootFolder2File1, rootFolder2SubfolderFile1, rootFile1, rootFolder1SubfolderFile1, rootFolder1File1));
		header.setTransformedFiles(Collections.singletonList(rootFile1Transformed));
	}

	@Test
	public void shouldImportFilesAlphabeticallyFollowedByDirectoriesAlphabetically()
	{
		service.execute(header);

		InOrder orderOf = inOrder(setupImpexService);
		verifyFileWasImported(orderOf, rootFile1Transformed);
		verifyFileWasImported(orderOf, rootFolder1File1);
		verifyFileWasImported(orderOf, rootFolder1SubfolderFile1);
		verifyFileWasImported(orderOf, rootFolder1File2);
		verifyFileWasImported(orderOf, rootFolder2SubfolderFile1);
		verifyFileWasImported(orderOf, rootFolder2File1);
		verifyFileWasImported(orderOf, rootFile2);
	}

	@Test
	public void shouldHandleUnzippedFilesBeingNull()
	{
		header.setUnzippedFiles(null);

		service.execute(header);

		InOrder orderOf = inOrder(setupImpexService);
		verifyFileWasImported(orderOf, rootFile1Transformed);
	}

	@Test
	public void shouldHandleTransformFilesBeingNull()
	{
		header.setTransformedFiles(null);

		service.execute(header);

		InOrder orderOf = inOrder(setupImpexService);
		verifyFileWasImported(orderOf, rootFolder1File1);
		verifyFileWasImported(orderOf, rootFolder1SubfolderFile1);
		verifyFileWasImported(orderOf, rootFolder1File2);
		verifyFileWasImported(orderOf, rootFolder2SubfolderFile1);
		verifyFileWasImported(orderOf, rootFolder2File1);
		verifyFileWasImported(orderOf, rootFile2);
	}

	private void verifyFileWasImported(final InOrder orderOf, final File expectedFile)
	{
		orderOf.verify(setupImpexService).importImpexFile("/" + expectedFile.getAbsolutePath(), true);
	}

}