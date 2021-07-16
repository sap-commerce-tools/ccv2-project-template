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
package de.hybris.platform.cloud.hotfolder;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.cloud.commons.services.monitor.*;
import de.hybris.platform.cloud.commons.spring.util.NeedsRunningSpringServices;
import de.hybris.platform.commerceservices.dataimport.impl.SampleDataImportService;
import de.hybris.platform.commerceservices.dataimport.impl.CoreDataImportService;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.impex.jalo.ImpExException;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.servicelayer.ExtendedServicelayerBaseTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.user.UserService;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import javax.annotation.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@IntegrationTest
@NeedsRunningSpringServices(roles = "integration")
public class CloudHotFolderIntegrationTests extends ExtendedServicelayerBaseTest
{
	private static final Logger LOG = getLogger(CloudHotFolderIntegrationTests.class);

	private static final String FILE_ROOT_DIR = "cloudhotfolder/test/";
	private static final String COPY_TO = "HYBRIS_TEMP_DIR";

	@Resource
	private ConfigurationService configurationService;

	@Resource
	private UserService userService;

	@Resource
	private MessageChannel hotfolderInboundFileHeaderEnricherChannel;

	@Resource
	private ProductService productService;

	@Resource
	private CatalogVersionService catalogVersionService;

	@Resource(name = "integrationTestMonitorAssertionHelper")
	private IntegrationTestMonitorAssertionHelper monitorAssertionHelper;

	private String copyToDir;

	private CatalogVersionModel catalogVersion;

	@Before
	public void setUp() throws Exception
	{
		copyToDir = configurationService.getConfiguration().getString(COPY_TO);
		Assert.notNull(copyToDir, "copyToDir cannot be null");

		runCleanUpScript();
		createCoreData();
		importStream(getResourceAsStream("additionalTestData.csv"), "UTF-8", FILE_ROOT_DIR + "additionalTestData.csv");
		catalogVersion = catalogVersionService.getCatalogVersion("testProductCatalog", "Staged");
	}

	@After
	public void teardown() throws ImpExException
	{
		runCleanUpScript();
	}

	private void runCleanUpScript() throws ImpExException
	{
		importStream(getResourceAsStream("cleanup.impex"), "UTF-8", FILE_ROOT_DIR + "cleanup.impex");
	}

	private InputStream getResourceAsStream(final String path)
	{
		return this.getClass().getClassLoader().getResourceAsStream(FILE_ROOT_DIR + path);
	}

	private File getResourceAsFile(final String path) throws IOException
	{
		final ClassLoader classLoader = this.getClass().getClassLoader();
		final String fullPath = FILE_ROOT_DIR + path;
		File file = null;
		final URL url = classLoader.getResource(fullPath);
		if (url != null)
		{
			file = new File(url.getFile());
		}
		if (file == null || !file.exists() || !file.isFile())
		{
			throw new FileNotFoundException("File not found: " + fullPath);
		}
		final File copyOf = new File(String.join(File.separator, copyToDir, path));
		//Make a copy of the file so the source file isn't deleted
		FileUtils.copyFile(file, copyOf);
		return copyOf;
	}

	private boolean productExists(final String code)
	{
		return getProduct(code) != null;
	}

	private ProductModel getProduct(final String code)
	{
		try
		{
			return productService.getProductForCode(catalogVersion, code);
		}
		catch (final UnknownIdentifierException e)
		{
			return null;
		}
	}

	private void ensureHistoryIsPresent(final String key,
	                                    final int expectedTransformedFiles,
	                                    final int expectedAttemptingToProcess,
	                                    final List<Integer> indicesOfSuccessfulImportActions,
	                                    final Optional<Step> initialStep)
	{
		final List<MonitorHistoryEntryData> steps = new LinkedList<>();
		initialStep.ifPresent(s -> steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, s)));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_SETUP));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_INIT));

		final List<MonitorHistoryEntryActionData> transformActions = new LinkedList<>();
		for (int i = 0; i < expectedTransformedFiles; i++)
		{
			transformActions.add(monitorAssertionHelper.createAction(Status.SUCCESS));
		}
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_TRANSFORMED, transformActions));

		final List<MonitorHistoryEntryActionData> executionActions = getOrderedInProgressSuccessfulActions(expectedAttemptingToProcess, indicesOfSuccessfulImportActions);

		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_EXECUTED, executionActions));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_CLEANUP));

		final MonitorHistoryData expectedHistory = monitorAssertionHelper.createHistory(key, SystemArea.HOT_FOLDER, Status.SUCCESS, steps);
		monitorAssertionHelper.assertHistoryIsPresent(expectedHistory);
	}

	private List<MonitorHistoryEntryActionData> getOrderedInProgressSuccessfulActions(final int expectedAttemptingToProcess,
	                                                                                final List<Integer> indicesOfSuccessfulImportActions)
	{
		final List<MonitorHistoryEntryActionData> executionActions = new ArrayList<>();
		for (int i = 0; i < expectedAttemptingToProcess; i++)
		{
			executionActions.add(monitorAssertionHelper.createAction(Status.IN_PROGRESS));
		}

		for (int index : indicesOfSuccessfulImportActions)
		{
			executionActions.add(index, monitorAssertionHelper.createAction(Status.SUCCESS));
		}
		return executionActions;
	}

	private boolean isExtensionLoaded(final String extensionNameToCheck)
	{
		final List<String> loadedExtensionNames = Registry.getCurrentTenant().getTenantSpecificExtensionNames();
		return loadedExtensionNames.contains(extensionNameToCheck);
	}

	@Test
	public void shouldImportCustomerCsvSuccessfully() throws IOException
	{
		final String testFile = "hotfolder/customer-00.csv";
		final String expectedUid = "bob@job.com";

		// Given our testFile is a CSV expect a conversion to impex
		final int expectedFilesTransformedToImpex = 1;

		final List<Integer> indicesOfSuccessfulImportActions = Lists.newArrayList(0); // Our transformed csv file will be executed first...
		final int expectedAttemptingToProcess = 0; // Can only offer attempt statement for zip files...

		assertThat(userService.isUserExisting(expectedUid)).isFalse();

		final File file = getResourceAsFile(testFile);
		final String monitorKey = file.getName() + file.lastModified();
		final Message<File> message = MessageBuilder.withPayload(file).build();

		assertThat(hotfolderInboundFileHeaderEnricherChannel.send(message))
				.as("Message not sent for file: " + testFile)
				.isTrue();

		assertThat(userService.isUserExisting(expectedUid))
				.as("Expected user to exist, but did not. UID: " + expectedUid)
				.isTrue();

		ensureHistoryIsPresent(monitorKey, expectedFilesTransformedToImpex, expectedAttemptingToProcess, indicesOfSuccessfulImportActions, Optional.empty());
	}

	@Test
	public void shouldImportProductCsvSuccessfully() throws IOException
	{
		final String testFile = "hotfolder/product-00.csv";
		final String expectedId = "123456";

		// Given our testFile is a CSV expect a conversion to impex
		final int expectedFilesTransformedToImpex = 1;

		/**
		 * See {@link CoreDataImportService} for details of executed impex, their ordering and other optional impexes
		 */
		final List<Integer> indicesOfSuccessfulImportActions = Lists.newArrayList(0); // Our transformed csv file will be executed first...
		final int expectedAttemptingToProcess = 0; // Can only offer attempt statement for zip files...

		assertThat(productExists(expectedId)).isFalse();

		final File file = getResourceAsFile(testFile);
		final String monitorKey = file.getName() + file.lastModified();
		final Message<File> message = MessageBuilder.withPayload(file).build();

		assertThat(hotfolderInboundFileHeaderEnricherChannel.send(message))
				.as("Message not sent for file: " + testFile)
				.isTrue();

		assertThat(productExists(expectedId))
				.as("Expected product to exist, but did not. Code: " + expectedId)
				.isTrue();

		ensureHistoryIsPresent(monitorKey, expectedFilesTransformedToImpex, expectedAttemptingToProcess, indicesOfSuccessfulImportActions, Optional.empty());
	}

	@Test
	public void shouldImportCoreDataZipSuccessfully() throws IOException
	{
		final String testFile = "coredata.zip";
		final String expectedUid = "essentia.data@domain.com";

		// Only CSVs are converted to impex and our test zip (coredata.zip) contains none!
		final int expectedFilesTransformedToImpex = 0;

		/**
		 * See {@link CoreDataImportService} for details of executed impex, their ordering and other optional impexes
		 */
		final List<Integer> indicesOfSuccessfulImportActions = Lists.newArrayList(1);  // essential-data.impex
		final int expectedAttemptingToProcess = 16;

		assertThat(userService.isUserExisting(expectedUid)).isFalse();

		final File file = getResourceAsFile(testFile);
		final String monitorKey = file.getName() + file.lastModified();
		final Message<File> message = MessageBuilder.withPayload(file).build();

		assertThat(hotfolderInboundFileHeaderEnricherChannel.send(message))
				.as("Message not sent for file: " + testFile)
				.isTrue();

		assertThat(userService.isUserExisting(expectedUid))
				.as("Expected user to exist, but did not. UID: " + expectedUid)
				.isTrue();

		ensureHistoryIsPresent(monitorKey, expectedFilesTransformedToImpex, expectedAttemptingToProcess, indicesOfSuccessfulImportActions, Optional.of(Step.FILE_UNZIPPED));
	}

	@Test
	public void shouldImportSampleDataZipSuccessfully() throws IOException
	{
		final String csBackOffice = "customersupportbackoffice";
		final String testZip = "sampledata.zip";

		// Only CSVs are converted to impex and our test zip (sampledata.zip) contains none!
		final int expectedFilesTransformedToImpex = 0;

		/**
		 * See {@link SampleDataImportService} for details of executed impex, their ordering and other optional impexes for extension {@link csBackOffice}
		 */
		final List<Integer> indicesOfSuccessfulImportActions = Lists.newArrayList(1); // customersupport-groups.impex
		final int expectedAttemptingToProcess = 40;

		if (isExtensionLoaded(csBackOffice))
		{
			final String expectedUid = "sample.data@domain.com";

			assertThat(userService.isUserExisting(expectedUid)).isFalse();

			final File file = getResourceAsFile(testZip);
			final String monitorKey = file.getName() + file.lastModified();
			final Message<File> message = MessageBuilder.withPayload(file).build();

			assertThat(hotfolderInboundFileHeaderEnricherChannel.send(message))
					.as("Message not sent for file: " + testZip)
					.isTrue();

			assertThat(userService.isUserExisting(expectedUid))
					.as("Expected user to exist, but did not. UID: " + expectedUid)
					.isTrue();

			ensureHistoryIsPresent(monitorKey, expectedFilesTransformedToImpex, expectedAttemptingToProcess, indicesOfSuccessfulImportActions, Optional.of(Step.FILE_UNZIPPED));
		}
		else
		{
			LOG.warn("Extension [{}] is not loaded so importing [{}] will not work.  Skipping test", csBackOffice, testZip);
		}
	}

	@Test
	public void shouldImportRawImpexZipSuccessfully() throws IOException
	{
		final String testFile = "rawimpex.zip";
		final String expectedUid = "first.job@domain.com";
		final String expectedUid2 = "second.job@domain.com";
		final String expectedUid3 = "third.job@domain.com";

		// Only CSVs are converted to impex and our test zip (rawimpex.zip) contains none!
		final int expectedFilesTransformedToImpex = 0;

		// first_file.impex, second_file.impex and third_file.impex
		// SUCCESS follows IN_PROGRESS, hence off numbered indices
		final List<Integer> indicesOfSuccessfulImportActions = Lists.newArrayList(1, 3, 5);
		final int expectedAttemptingToProcess = 3;

		assertThat(userService.isUserExisting(expectedUid)).isFalse();
		assertThat(userService.isUserExisting(expectedUid2)).isFalse();
		assertThat(userService.isUserExisting(expectedUid3)).isFalse();

		final File file = getResourceAsFile(testFile);
		final String monitorKey = file.getName() + file.lastModified();
		final Message<File> message = MessageBuilder.withPayload(file).build();

		assertThat(hotfolderInboundFileHeaderEnricherChannel.send(message))
				.as("Message not sent for file: " + testFile)
				.isTrue();

		final SoftAssertions softly = new SoftAssertions();

		softly.assertThat(userService.isUserExisting(expectedUid))
				.as("Expected user to exist, but did not. UID: " + expectedUid)
				.isTrue();

		softly.assertThat(userService.isUserExisting(expectedUid2))
				.as("Expected user to exist, but did not. UID: " + expectedUid2)
				.isTrue();

		softly.assertThat(userService.isUserExisting(expectedUid3))
				.as("Expected user to exist, but did not. UID: " + expectedUid3)
				.isTrue();

		softly.assertAll();

		ensureHistoryIsPresent(monitorKey, expectedFilesTransformedToImpex, expectedAttemptingToProcess, indicesOfSuccessfulImportActions, Optional.of(Step.FILE_UNZIPPED));
	}

	@Test
	public void shouldImportCsvAndImpexZipSuccessfully() throws IOException
	{
		final String testFile = "product-with-media.zip";

		final String expectedProductId = "112233";
		final String expectedUid = "raw.impex@domain.com";

		// product.csv and zip_media.csv
		final int expectedFilesTransformedToImpex = 2;

		// raw.impex, product.impex, zip_media_1.impex, zip_media_2.impex, zip_media_3.impex
		// SUCCESS follows IN_PROGRESS, hence off numbered indices
		final List<Integer> indicesOfSuccessfulImportActions = Lists.newArrayList(1, 3, 5, 7, 9);
		final int expectedAttemptingToProcess = 5;

		assertThat(productExists(expectedProductId)).isFalse();
		assertThat(userService.isUserExisting(expectedUid)).isFalse();

		final File file = getResourceAsFile(testFile);
		final String monitorKey = file.getName() + file.lastModified();
		final Message<File> message = MessageBuilder.withPayload(file).build();

		final SoftAssertions softly = new SoftAssertions();

		softly.assertThat(hotfolderInboundFileHeaderEnricherChannel.send(message))
				.as("Message not sent for file: " + testFile)
				.isTrue();

		softly.assertThat(productExists(expectedProductId))
				.as("Expected product to exist, but did not. Code: " + expectedProductId)
				.isTrue();

		softly.assertThat(userService.isUserExisting(expectedUid))
				.as("Expected user to exist, but did not. UID: " + expectedUid)
				.isTrue();

		softly.assertAll();

		ensureHistoryIsPresent(monitorKey, expectedFilesTransformedToImpex, expectedAttemptingToProcess, indicesOfSuccessfulImportActions, Optional.of(Step.FILE_UNZIPPED));
	}

	@Test
	public void shouldHandleUnmappedGracefully() throws IOException
	{
		final String testFile = "hotfolder/unknownfile-00.csv";

		final File file = getResourceAsFile(testFile);
		final String monitorKey = file.getName() + file.lastModified();
		final Message<File> message = MessageBuilder.withPayload(file).build();
		hotfolderInboundFileHeaderEnricherChannel.send(message);
		final MonitorHistoryEntryData notRouted = monitorAssertionHelper.createStep(Status.FAILURE, Step.FILE_ROUTED);
		final MonitorHistoryData expectedHistory
				= monitorAssertionHelper.createHistory(monitorKey, SystemArea.HOT_FOLDER, Status.WARNING, notRouted);
		monitorAssertionHelper.assertHistoryIsPresent(expectedHistory);
	}


}
