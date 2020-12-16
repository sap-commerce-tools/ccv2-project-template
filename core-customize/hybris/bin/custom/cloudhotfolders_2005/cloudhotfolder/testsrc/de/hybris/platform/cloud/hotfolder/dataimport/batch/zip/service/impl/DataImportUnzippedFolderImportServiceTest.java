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
import de.hybris.platform.commerceservices.dataimport.AbstractDataImportService;
import de.hybris.platform.commerceservices.setup.AbstractSystemSetup;
import de.hybris.platform.commerceservices.setup.data.ImportData;
import de.hybris.platform.core.initialization.SystemSetup;
import de.hybris.platform.core.initialization.SystemSetupContext;

import java.io.File;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DataImportUnzippedFolderImportServiceTest
{
	private static final String UNZIPPED_FOLDER_NAME = "be24ef21-cc29-5d71-d7bd-93c35baa2d3b";
	private static final File UNZIPPED_DIRECTORY = new File("/workDirectory" + File.separator + UNZIPPED_FOLDER_NAME);
	private static final String FILE_UNZIPPED_AS = "file_unzipped_as";
	private static final File UNZIPPED_AS = new File(UNZIPPED_DIRECTORY, FILE_UNZIPPED_AS);
	private static final String REQUIRED_SUBDIRECTORY = "/import/coredata";

	@Spy
	private DataImportUnzippedFolderImportService service = new DataImportUnzippedFolderImportService();

	@Mock
	private AbstractDataImportService abstractDataImportService;

	@Mock
	private AbstractSystemSetup abstractSystemSetup;

	private ImportData importData = new ImportData();

	private SystemSetupContext expectedContext;

	private ZipBatchHeader header = new ZipBatchHeader();

	@Before
	public void setUp()
	{
		service.setAbstractDataImportService(abstractDataImportService);
		service.setAbstractSystemSetup(abstractSystemSetup);
		service.setImportDatas(Collections.singletonList(importData));
		service.setRequiredSubdirectory(REQUIRED_SUBDIRECTORY);

		header.setFileUnzippedAs(UNZIPPED_AS);

		expectedContext = new SystemSetupContext(Collections.emptyMap(),
				SystemSetup.Type.NOTDEFINED,
				SystemSetup.Process.NOTDEFINED,
				UNZIPPED_AS.getAbsolutePath());
	}

	@Test
	public void shouldTryToImportCoreDataWhenFolderFoundWithinWorkingFolder()
	{
		willReturn(true).given(service).requiredDirExists(UNZIPPED_AS, "/import/coredata");

		service.execute(header);

		verify(abstractDataImportService).execute(eq(abstractSystemSetup), refEq(expectedContext), eq(Collections.singletonList(importData)));
	}

	@Test
	public void shouldNotUseImportServicesWhenFolderNotFoundWithinWorkingFolder()
	{
		willReturn(false).given(service).requiredDirExists(eq(UNZIPPED_AS), anyString());

		service.execute(header);

		verify(abstractDataImportService, never()).execute(eq(abstractSystemSetup), refEq(expectedContext), eq(Collections.singletonList(importData)));
	}
}