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
package de.hybris.platform.cloud.azure.hotfolder.aop;

import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.azure.hotfolder.remote.session.AzureBlobSession;
import de.hybris.platform.cloud.commons.aop.exception.StepException;
import de.hybris.platform.cloud.commons.services.monitor.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class BlobTransferredAspectTest
{
    private static final String PATH = "/container/folder";
    private static final long MODIFIED = new Date().getTime();
    private static final long SIZE = 200L;
    private static final String FILE_NAME = "fileName.csv";

    private final BlobTransferredAspect aspect = new BlobTransferredAspect();

    @Mock
    private MonitorService monitorService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProceedingJoinPoint pjp;

    @Mock
    private AzureBlobSession session;

    private final Object result = new Object();

    @Mock
    private MonitorHistory monitorHistory;


    @Before
    public void setUp() throws Throwable
    {
        aspect.setMonitorService(monitorService);

        given(pjp.getTarget()).willReturn(session);
        given(pjp.proceed()).willReturn(result);

        // BlobProperties is final, and cannot be mocked.  The setters are protected,
        // so the only easy way to set the properties we need is via reflection.
        final BlobProperties entryProperties = new BlobProperties();
        ReflectionTestUtils.setField(entryProperties, "lastModified", new Date(MODIFIED));
        ReflectionTestUtils.setField(entryProperties, "length", SIZE);

        // Most methods on CloudBlob are final, and cannot be mocked.  The setters are protected,
        // so the only easy way to set the properties we need is via reflection.
        final CloudBlockBlob entry = new CloudBlockBlob(new URI("http", "myserver",PATH + "/" + FILE_NAME, null));
        ReflectionTestUtils.setField(entry, "properties", entryProperties);
        ReflectionTestUtils.setField(entry, "name", PATH + "/" + FILE_NAME);

        when(session.get(PATH)).thenReturn(entry);

        given(monitorService.begin(SystemArea.HOT_FOLDER, FILE_NAME + MODIFIED)).willReturn(monitorHistory);
        given(monitorService.current()).willReturn(Optional.of(monitorHistory));
    }

    @Test
    public void shouldReturnResultFromExecutedMethod() throws Throwable
    {
        assertThat(aspect.aroundRead(pjp, PATH)).isEqualTo(result);
    }

    @Test
    public void givenMethodWasSuccessful_thenShouldLogSuccessfulStep_andIssueCheckpoint() throws Throwable
    {
        aspect.aroundRead(pjp, PATH);

        final InOrder order = inOrder(monitorHistory);
        order.verify(monitorHistory).stepSucceeded(eq(Step.DOWNLOADED), isA(Date.class), isA(Date.class),
                eq("Successfully read blob [{}] of size [{}]"), eq(PATH), eq(SIZE + " bytes"));
        order.verify(monitorHistory).checkpoint();
        verifyNoMoreInteractions(monitorHistory);
    }

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException runtimeException = new RuntimeException();
		given(pjp.proceed()).willThrow(runtimeException);

		expectedException.expect(Matchers.isA(StepException.class));
		expectedException.expectCause(is(runtimeException));

        aspect.aroundRead(pjp, PATH);

		final InOrder order = inOrder(monitorHistory);
		order.verify(monitorHistory).stepFailed(eq(Step.DOWNLOADED), isA(Date.class), isA(Date.class),
				eq(runtimeException), eq("Failed to read read blob [{}]"), eq(PATH));
		order.verify(monitorHistory).end(Status.FAILURE);
		verifyNoMoreInteractions(monitorHistory);
	}
}