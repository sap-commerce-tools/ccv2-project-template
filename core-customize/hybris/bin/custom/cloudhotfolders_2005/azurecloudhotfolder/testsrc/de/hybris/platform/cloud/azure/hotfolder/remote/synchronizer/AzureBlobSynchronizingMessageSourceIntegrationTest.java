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
package de.hybris.platform.cloud.azure.hotfolder.remote.synchronizer;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.cloud.azure.hotfolder.remote.session.TestBlobSession;
import de.hybris.platform.cloud.commons.services.monitor.*;
import de.hybris.platform.cloud.commons.spring.util.NeedsRunningSpringServices;
import de.hybris.platform.servicelayer.ExtendedServicelayerBaseTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.annotation.Resource;

import java.io.File;
import java.net.URL;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@IntegrationTest
@NeedsRunningSpringServices(roles = {"integration", "yHotfolderServices"})
public class AzureBlobSynchronizingMessageSourceIntegrationTest extends ExtendedServicelayerBaseTest
{
	private static final Logger LOG = getLogger(AzureBlobSynchronizingMessageSourceIntegrationTest.class);

	private static final String POLL_FREQ_KEY = "azure.hotfolder.storage.polling.fixed.rate";
	private static final String FILE_ROOT_DIR = "azurecloudhotfolder/test/";

	@Resource
	private TestBlobSession testBlobSession;

	@Resource
	private ConfigurationService configurationService;

	@Resource
	private UserService userService;

	@Resource
	private ModelService modelService;

	@Resource(name = "integrationTestMonitorAssertionHelper")
	private IntegrationTestMonitorAssertionHelper monitorAssertionHelper;

	private int pollingFrequency;

	private final String expectedUid = "azure.integrationtest@domain.com";

	@Before
	public void setUp() throws Exception
	{
		pollingFrequency = configurationService.getConfiguration().getInt(POLL_FREQ_KEY, 1000);
		removeTestUser(expectedUid);
		createCoreData();
	}

	@After
	public void teardown()
	{
		removeTestUser(expectedUid);
	}

	private File setSessionToReturnFile(final String path)
	{
		final ClassLoader classLoader = this.getClass().getClassLoader();
		final File file = Optional.of(path)
				.map(classLoader::getResource)
				.map(URL::getFile)
				.map(File::new)
				.filter(File::exists)
				.orElseThrow(() -> new RuntimeException("File not found: " + path));

		testBlobSession.setFiles(Collections.singletonList(file));
		return file;
	}

	@Test
	public void shouldDownloadRemoteFilesAndImportCustomerCsvSuccessfully() throws InterruptedException
	{
		assertThat(checkUserExists(expectedUid)).isFalse();

		final String testFile = "customer-00.csv";
		File file = setSessionToReturnFile(FILE_ROOT_DIR + testFile);
		file.setLastModified((new Date().getTime()));
		final String monitorKey = file.getName() + file.lastModified();

		Thread.sleep(pollingFrequency);
		int current = 0, maxAttempts = 5;
		boolean read = false;
		while (!read && current < maxAttempts)
		{
			current++;
			read = testBlobSession.fileRead(testFile);
			if (!read)
			{
				LOG.info("file [{}] wasn't read yet, will wait [{}]ms for poller", testFile, pollingFrequency);
				Thread.sleep(pollingFrequency);
			}
		}

		assertThat(read).as(String.format("File [%s] was not read", testFile)).isTrue();

		if (!checkUserExists(expectedUid))
		{
			LOG.info("user [{}] doesn't exists yet maybe processing was incomplete, will wait [{}]ms for poller", expectedUid, pollingFrequency);
			Thread.sleep(pollingFrequency);
			assertThat(checkUserExists(expectedUid)).isTrue();
		}

		ensureHistoryIsPresent(monitorKey);
	}

	private boolean checkUserExists(final String uid)
	{
		return userService.isUserExisting(uid);
	}

	private void removeTestUser(final String uid)
	{
		testBlobSession.clean();
		if (checkUserExists(expectedUid))
		{
			modelService.remove(userService.getUserForUID(uid));
		}
	}

	private void ensureHistoryIsPresent(final String key)
	{
		final List<MonitorHistoryEntryData> steps = new LinkedList<>();
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.DOWNLOADED));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_SETUP));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_INIT));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_TRANSFORMED, monitorAssertionHelper.createAction(Status.SUCCESS)));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_EXECUTED, monitorAssertionHelper.createAction(Status.SUCCESS)));
		steps.add(monitorAssertionHelper.createStep(Status.SUCCESS, Step.HEADER_CLEANUP));

		final MonitorHistoryData expectedHistory = monitorAssertionHelper.createHistory(key, SystemArea.HOT_FOLDER, Status.SUCCESS, steps);
		monitorAssertionHelper.assertHistoryIsPresent(expectedHistory);
	}

}
