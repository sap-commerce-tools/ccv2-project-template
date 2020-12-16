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
import de.hybris.platform.cloud.commons.aop.exception.ActionException;
import de.hybris.platform.cloud.commons.services.monitor.MonitorHistory;
import de.hybris.platform.cloud.commons.services.monitor.MonitorService;
import de.hybris.platform.cloud.commons.services.monitor.Status;
import de.hybris.platform.servicelayer.impex.ImportConfig;
import de.hybris.platform.servicelayer.impex.ImportResult;

import java.util.Date;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class ImpexImportAspectTest
{
	private static final String SCRIPT_MEDIA_CODE = "scriptMediaCode";

	private ImpexImportAspect aspect = new ImpexImportAspect();

	private String FILE_ABS_PATH = "filepath/filename.csv";
	private String FILE_NAME = "filename.csv";
	private String OPTIONAL = "OPTIONAL";
	private String MANDATORY = "MANDATORY";

	@Mock
	private MonitorService monitorService;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ProceedingJoinPoint pjp;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ImportResult result;

	@Mock
	private MonitorHistory monitorHistory;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ImportConfig config;

	@Captor
	private ArgumentCaptor<String> actionCodeCaptor;

	@Before
	public void setUp() throws Throwable
	{
		aspect.setMonitorService(monitorService);

		given(pjp.proceed()).willReturn(result);

		given(monitorService.current()).willReturn(Optional.of(monitorHistory));

		given(config.getScript().getMedia().getCode()).willReturn(SCRIPT_MEDIA_CODE);
	}

	@Test
	public void importDataMethod_shouldReturnResultFromExecutedMethod() throws Throwable
	{
		assertThat(aspect.aroundImportData(pjp, config)).isEqualTo(result);
	}

	@Test
	public void setupImpexMethod_shouldReturnResultFromExecutedMethod() throws Throwable
	{
		assertThat(aspect.aroundSetupImpex(pjp, FILE_ABS_PATH, false)).isEqualTo(result);
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void importDataMethod_givenMethodFailed_thenShouldLogFailedStep_andIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException("error Message");
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(ActionException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundImportData(pjp, config);

		verify(monitorHistory).addAction(eq(SCRIPT_MEDIA_CODE), eq(Status.FAILURE), isA(Date.class), isA(Date.class),
				eq("Impex didn't run successfully and exception was thrown.  Ex Message: {}"), eq(ex.getMessage()));
		verifyNoMoreInteractions(monitorHistory);
	}

	@Test
	public void impexSetupMethod_givenMethodFailed_thenShouldNotLogFailedStep_andNotIssueEndMonitor() throws Throwable
	{
		final RuntimeException ex = new RuntimeException("error Message");
		given(pjp.proceed()).willThrow(ex);

		expectedException.expect(org.hamcrest.Matchers.isA(ActionException.class));
		expectedException.expectCause(is(ex));

		aspect.aroundSetupImpex(pjp, FILE_ABS_PATH, false);

		verifyZeroInteractions(monitorHistory);
	}

	@Test
	public void importDataMethod_givenMethodWasSuccessful_andScriptWasSuccessful_thenShouldLogSuccessfulAction() throws Throwable
	{
		given(result.isSuccessful()).willReturn(true);

		aspect.aroundImportData(pjp, config);

		verify(monitorHistory).addAction(eq(SCRIPT_MEDIA_CODE), eq(Status.SUCCESS), isA(Date.class), isA(Date.class), eq("Impex ran successfully"));
		verifyNoMoreInteractions(monitorHistory);
	}

	@Test
	public void impexSetupMethod_givenMethodWasSuccessful_andFileIsOptional_thenShouldLogSuccessfulAction() throws Throwable
	{
		aspect.aroundSetupImpex(pjp, FILE_ABS_PATH, false);

		verify(monitorHistory).addAction(actionCodeCaptor.capture(), eq(Status.IN_PROGRESS), isA(Date.class), isA(Date.class),
				eq("Attempting to process [{}] impex file [{}]..."), eq(OPTIONAL), eq(FILE_NAME));
		verifyNoMoreInteractions(monitorHistory);
	}

	@Test
	public void impexSetupMethod_givenMethodWasSuccessful_andFileIsMandatory_thenShouldLogSuccessfulAction() throws Throwable
	{
		aspect.aroundSetupImpex(pjp, FILE_ABS_PATH, true);

		verify(monitorHistory).addAction(actionCodeCaptor.capture(), eq(Status.IN_PROGRESS), isA(Date.class), isA(Date.class),
				eq("Attempting to process [{}] impex file [{}]..."), eq(MANDATORY), eq(FILE_NAME));
		verifyNoMoreInteractions(monitorHistory);
	}

	@Test
	public void givenMethodWasSuccessful_butScriptWasUnsuccessful_thenShouldLogErrorAction() throws Throwable
	{
		given(result.isError()).willReturn(true);
		given(result.hasUnresolvedLines()).willReturn(true);
		given(result.getUnresolvedLines().getPreview()).willReturn("unresolved preview");

		aspect.aroundImportData(pjp, config);

		verify(monitorHistory).addAction(eq(SCRIPT_MEDIA_CODE), eq(Status.FAILURE), isA(Date.class), isA(Date.class),
				eq("Impex didn't run successfully. {}"), eq("unresolved preview"));
		verifyNoMoreInteractions(monitorHistory);
	}

	@Test
	public void importDataMethod_givenMethodWasSuccessful_butScriptIsStillRunning_thenShouldLogInProgressAction() throws Throwable
	{
		given(result.isRunning()).willReturn(true);

		aspect.aroundImportData(pjp, config);

		verify(monitorHistory).addAction(eq(SCRIPT_MEDIA_CODE), eq(Status.IN_PROGRESS), isA(Date.class), isA(Date.class),
				eq("Impex is still running, will not wait.  Maybe consider setting config.synchronous to true"));
		verifyNoMoreInteractions(monitorHistory);
	}
}