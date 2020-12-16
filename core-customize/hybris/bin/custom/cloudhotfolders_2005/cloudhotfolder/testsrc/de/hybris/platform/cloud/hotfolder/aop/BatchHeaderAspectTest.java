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
import de.hybris.platform.acceleratorservices.dataimport.batch.BatchHeader;
import de.hybris.platform.cloud.commons.aop.exception.StepException;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistory;
import de.hybris.platform.cloud.commons.services.monitor.MonitorService;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.cloud.commons.services.monitor.Step;
import de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.ZipBatchHeader;

import java.io.File;
import java.util.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class BatchHeaderAspectTest
{
	private static final long MODIFIED = 1L;
	private static final String FILE_NAME = "fileName";
	private static final UUID ID = UUID.randomUUID();
	private static final long DEFAULT_SEQ_ID = 0L;

	private BatchHeaderAspect aspect = new BatchHeaderAspect();

	@Mock
	private MonitorService monitorService;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ProceedingJoinPoint pjp;

	private Object result = new Object();

	private ZipBatchHeader zipHeaderResult = new ZipBatchHeader();
	private BatchHeader batchHeaderResult = new BatchHeader();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private Message<?> message;

	@Mock
	private MonitorHistory monitorHistory;

	@Mock
	private ZipBatchHeader zipHeader;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private BatchHeader batchHeader;

	@Captor
	private ArgumentCaptor<String> actionCodeCaptor;

	@Before
	public void setUp() throws Throwable
	{
		aspect.setMonitorService(monitorService);
		aspect.setFileNameHeaderKey("nameKey");

		given(pjp.proceed()).willReturn(result);

		given(message.getHeaders().getId()).willReturn(ID);
		given(message.getHeaders().get("nameKey")).willReturn(FILE_NAME);
		given(message.getHeaders().get("modifiedKey")).willReturn(MODIFIED);

		given(monitorService.current()).willReturn(Optional.of(monitorHistory));

		given(zipHeader.getOriginalFileName()).willReturn(FILE_NAME);
		given(batchHeader.getFile().getName()).willReturn(FILE_NAME);
	}

	@Test
	public void setup_shouldReturnResultFromExecutedMethod() throws Throwable
	{
		assertThat(aspect.aroundMessageSetup(pjp, message)).isEqualTo(result);
	}

	@Test
	public void setup_givenMethodWasSuccessful_thenShouldLogSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		aspect.aroundMessageSetup(pjp, message);

		verifySuccessRecorded(Step.HEADER_SETUP, "Successfully setup header for file [{}]. Header will be passed down pipeline.....");
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void setup_givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException();
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(StepException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundMessageSetup(pjp, message);

		verifyFailureRecorded(Step.HEADER_SETUP, ex, "Failed to setup header for file [{}]");
	}

	@Test
	public void init_shouldReturnResultFromExecutedMethod() throws Throwable
	{
		assertThat(aspect.aroundInit(pjp, zipHeader)).isEqualTo(result);
		assertThat(aspect.aroundInit(pjp, batchHeader)).isEqualTo(result);
	}

	@Test
	public void init_givenMethodWasSuccessful_thenShouldLogSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		aspect.aroundInit(pjp, zipHeader);
		aspect.aroundInit(pjp, batchHeader);

		final String message = "Successfully processed file [{}]. SequenceId [{}]";

		verify(monitorHistory, times(2))
				.stepSucceeded(eq(Step.HEADER_INIT), isA(Date.class), isA(Date.class), eq(message), eq(FILE_NAME), eq(DEFAULT_SEQ_ID));
		verifyNoMoreInteractions(monitorHistory);
	}

	@Test
	public void init_givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException();
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(StepException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundInit(pjp, zipHeader);

		verifyFailureRecorded(Step.HEADER_SETUP, ex, "Failed to process file [{}]");
	}

	@Test
	public void transform_shouldReturnResultFromExecutedMethod() throws Throwable
	{
		given(pjp.proceed()).willReturn(zipHeaderResult);

		assertThat(aspect.aroundTransform(pjp, zipHeader)).isEqualTo(zipHeaderResult);

		given(pjp.proceed()).willReturn(batchHeaderResult);

		assertThat(aspect.aroundTransform(pjp, batchHeader)).isEqualTo(batchHeaderResult);
	}

	@Test
	public void transform_givenMethodWasSuccessful_thenShouldLogSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		given(pjp.proceed()).willReturn(zipHeaderResult);

		aspect.aroundTransform(pjp, zipHeader);

		verifySuccessRecordedWithSequenceId(Step.HEADER_TRANSFORMED, "Successfully transformed header for zip folder [{}] with sequenceId [{}]");
	}

	@Test
	public void transform_givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException();
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(StepException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundTransform(pjp, zipHeader);

		verifyFailureRecorded(Step.HEADER_TRANSFORMED, ex, "Failed to process file [{}]");
	}

	@Test
	public void execute_shouldReturnResultFromExecutedMethod() throws Throwable
	{
		assertThat(aspect.aroundExecute(pjp, zipHeader)).isEqualTo(result);
	}

	@Test
	public void execute_givenMethodWasSuccessful_thenShouldLogSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		aspect.aroundExecute(pjp, zipHeader);

		verifySuccessRecordedWithSequenceId(Step.HEADER_EXECUTED, "Successfully processed file [{}]. SequenceId [{}]");
	}

	@Test
	public void execute_givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException();
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(StepException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundExecute(pjp, zipHeader);

		verifyFailureRecorded(Step.HEADER_EXECUTED, ex, "Failed to process file [{}]");
	}

	@Test
	public void cleanup_shouldReturnResultFromExecutedMethod() throws Throwable
	{
		assertThat(aspect.aroundCleanup(pjp, zipHeader)).isEqualTo(result);
		assertThat(aspect.aroundCleanup(pjp, batchHeader)).isEqualTo(result);
	}

	@Test
	public void cleanup_givenMethodWasSuccessful_thenShouldLogSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		aspect.aroundCleanup(pjp, zipHeader);
		aspect.aroundCleanup(pjp, batchHeader);

		final InOrder order = inOrder(monitorHistory);

		// Verify correct odering for zipHeader first
		order.verify(monitorHistory).stepSucceeded(eq(Step.HEADER_CLEANUP), isA(Date.class), isA(Date.class), eq("Successfully processed file [{}]. " +
				"SequenceId [{}]"), eq(FILE_NAME), eq(DEFAULT_SEQ_ID));
		order.verify(monitorHistory).end(Status.SUCCESS);

		// Verify correct ordering for batchHeader second
		order.verify(monitorHistory).stepSucceeded(eq(Step.HEADER_CLEANUP), isA(Date.class), isA(Date.class), eq("Successfully processed file [{}]. " +
				"SequenceId [{}]"), eq(FILE_NAME), eq(DEFAULT_SEQ_ID));
		order.verify(monitorHistory).end(Status.SUCCESS);

		verifyNoMoreInteractions(monitorHistory);
	}

	@Test
	public void cleanup_givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException();
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(StepException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundCleanup(pjp, zipHeader);

		verifyFailureRecorded(Step.HEADER_CLEANUP, ex, "Failed to process file [{}]");
	}

	@Test
	public void transform_batchHeader_givenMethodWasSuccessful_thenShouldLogSuccessFullActions_andSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		batchHeaderResult.addTransformedFile(new File("file1"));
		batchHeaderResult.addTransformedFile(new File("file2"));

		final List<String> transformedNames = Lists.newArrayList("file1", "file2");

		given(pjp.proceed()).willReturn(batchHeaderResult);

		aspect.aroundTransform(pjp, batchHeader);

		verify(monitorHistory).addAction(actionCodeCaptor.capture(), eq(Status.SUCCESS), isA(Date.class), isA(Date.class),
				eq("Successfully transformed file [{}] to impex(es) ({})"), eq(FILE_NAME), eq(transformedNames));

		assertThat(actionCodeCaptor.getValue()).hasSize(8);

		verifySuccessRecordedWithSequenceId(Step.HEADER_TRANSFORMED, "Successfully transformed header for file [{}] with sequenceId [{}]");
	}

	@Test
	public void transform_zipHeader_givenMethodWasSuccessful_thenShouldLogSuccessFullActions_andSuccessfulStep_andIssueCheckpoint() throws Throwable
	{
		zipHeaderResult.addOriginalToTransformedEntry("original_1", "transformed_1_1");
		zipHeaderResult.addOriginalToTransformedEntry("original_1", "transformed_1_2");
		zipHeaderResult.addOriginalToTransformedEntry("original_2", "transformed_2_1");

		final List<String> transformed_1 = Lists.newArrayList("transformed_1_1", "transformed_1_2");
		final List<String> transformed_2 = Lists.newArrayList("transformed_2_1");

		given(pjp.proceed()).willReturn(zipHeaderResult);

		aspect.aroundTransform(pjp, zipHeader);

		verify(monitorHistory).addAction(isA(String.class), eq(Status.SUCCESS), isA(Date.class), isA(Date.class),
				eq("For zip folder [{}], successfully transformed expanded file [{}] to impex(es) ({})"), eq(FILE_NAME), eq("original_1"), eq(transformed_1));

		verify(monitorHistory).addAction(isA(String.class), eq(Status.SUCCESS), isA(Date.class), isA(Date.class),
				eq("For zip folder [{}], successfully transformed expanded file [{}] to impex(es) ({})"), eq(FILE_NAME), eq("original_2"), eq(transformed_2));


		verifySuccessRecordedWithSequenceId(Step.HEADER_TRANSFORMED, "Successfully transformed header for zip folder [{}] with sequenceId [{}]");
	}

	private void verifySuccessRecordedWithSequenceId(final Step step, final String message)
	{
		verify(monitorHistory).stepSucceeded(eq(step), isA(Date.class), isA(Date.class), eq(message), eq(FILE_NAME), eq(DEFAULT_SEQ_ID));
		verifyNoMoreInteractions(monitorHistory);
	}

	private void verifySuccessRecorded(final Step step, final String message)
	{
		verify(monitorHistory).stepSucceeded(eq(step), isA(Date.class), isA(Date.class), eq(message), eq(FILE_NAME));
		verifyNoMoreInteractions(monitorHistory);
	}

	private void verifyFailureRecorded(final Step step, final Exception ex, final String message)
	{
		final InOrder order = inOrder(monitorHistory);
		order.verify(monitorHistory).stepFailed(eq(step), isA(Date.class), isA(Date.class), eq(ex), eq(message), eq(FILE_NAME));
		order.verify(monitorHistory).end(Status.FAILURE);
		verifyNoMoreInteractions(monitorHistory);

	}

}