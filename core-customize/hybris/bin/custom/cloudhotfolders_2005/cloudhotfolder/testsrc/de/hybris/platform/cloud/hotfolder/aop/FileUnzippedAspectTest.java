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
package de.hybris.platform.cloud.hotfolder.aop;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cloud.commons.aop.exception.StepException;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistory;
import de.hybris.platform.cloud.commons.services.monitor.MonitorService;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.commons.services.monitor.SystemArea;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class FileUnzippedAspectTest
{
	private static final long MODIFIED = 1L;
	private static final String FILE_NAME = "fileName";
	private static final UUID ID = UUID.randomUUID();

	private FileUnzippedAspect aspect = new FileUnzippedAspect();

	@Mock
	private MonitorService monitorService;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ProceedingJoinPoint pjp;

	private Object result = new Object();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private Message<?> message;

	@Mock
	private MonitorHistory monitorHistory;

	@Before
	public void setUp() throws Throwable
	{
		aspect.setMonitorService(monitorService);
		aspect.setFileNameHeaderKey("nameKey");
		aspect.setFileLastModifiedHeaderKey("modifiedKey");

		given(pjp.proceed()).willReturn(result);

		given(message.getHeaders().getId()).willReturn(ID);
		given(message.getHeaders().get("nameKey")).willReturn(FILE_NAME);
		given(message.getHeaders().get("modifiedKey")).willReturn(MODIFIED);

		given(monitorService.begin(SystemArea.HOT_FOLDER, FILE_NAME + MODIFIED)).willReturn(monitorHistory);
		given(monitorService.current()).willReturn(Optional.of(monitorHistory));
	}

	@Test
	public void shouldReturnResultFromExecutedMethod() throws Throwable
	{
		assertThat(aspect.aroundUnzipped(pjp, message)).isEqualTo(result);
	}

	@Test
	public void givenMethodWasSuccessful_thenShouldLogSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		aspect.aroundUnzipped(pjp, message);

		verify(monitorHistory).stepSucceeded(eq(Step.FILE_UNZIPPED), isA(Date.class), isA(Date.class),
				eq("Successfully unzipped file [{}] to [{}]"), eq(FILE_NAME), eq(ID.toString()));
		verifyNoMoreInteractions(monitorHistory);
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException();
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(StepException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundUnzipped(pjp, message);

		final InOrder order = inOrder(monitorHistory);
		order.verify(monitorHistory).stepFailed(eq(Step.FILE_UNZIPPED), isA(Date.class), isA(Date.class),
				eq(ex), eq("Failed to unzip file [{}]"), eq(FILE_NAME));
		order.verify(monitorHistory).end(Status.FAILURE);
		verifyNoMoreInteractions(monitorHistory);
	}
}