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
package de.hybris.platform.cloud.commons.services.monitor.impl;

import de.hybris.platform.cloud.commons.services.monitor.*;
import de.hybris.platform.core.Registry;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Required;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DefaultMonitorService implements MonitorService
{
	private final ThreadLocal<MonitorHistory> monitorHistory = new ThreadLocal<>();
	private MonitorRecorder monitorRecorder;
	private MonitorHistoryRestorer resumeMonitorRestorer;
	private MonitorHistoryFactory historyFactory;

	@Override
	public MonitorHistory begin(final SystemArea area, final String key)
	{
		final DefaultMonitorHistory history = new DefaultMonitorHistory(getHistoryFactory().create(area, key));
		monitorHistory.set(history);
		return history;
	}

	@Override
	public Optional<MonitorHistory> current()
	{
		return Optional.ofNullable(monitorHistory.get());
	}

	@Override
	public MonitorHistory resume(final SystemArea area, final String key)
	{
		final MonitorHistoryData history = getResumeMonitorRestorer().restore(area, key);
		final DefaultMonitorHistory historyMonitor = new DefaultMonitorHistory(history);
		monitorHistory.set(historyMonitor);
		return historyMonitor;
	}

	protected String formatMessage(final String message, final Object[] params)
	{
		return MessageFormatter.arrayFormat(message, params).getMessage();
	}

	protected int getClusterId()
	{
		return Registry.getClusterID();
	}

	protected MonitorRecorder getMonitorRecorder()
	{
		return monitorRecorder;
	}

	@Required
	public void setMonitorRecorder(final MonitorRecorder monitorRecorder)
	{
		this.monitorRecorder = monitorRecorder;
	}

	protected MonitorHistoryRestorer getResumeMonitorRestorer()
	{
		return resumeMonitorRestorer;
	}

	@Required
	public void setResumeMonitorRestorer(final MonitorHistoryRestorer resumeMonitorRestorer)
	{
		this.resumeMonitorRestorer = resumeMonitorRestorer;
	}

	protected MonitorHistoryFactory getHistoryFactory()
	{
		return historyFactory;
	}

	@Required
	public void setHistoryFactory(final MonitorHistoryFactory historyFactory)
	{
		this.historyFactory = historyFactory;
	}

	class DefaultMonitorHistory implements MonitorHistory
	{
		private final MonitorHistoryData history;
		private final LinkedList<MonitorHistoryEntryActionData> actions;

		public DefaultMonitorHistory(final MonitorHistoryData history)
		{
			this.history = history;
			this.actions = new LinkedList<>();
		}

		@Override
		public MonitorHistory addAction(final String code, final Status status, final Date start, final Date end, final String message, final Object... args)
		{
			actions.add(
					MonitorHistoryEntryActionData.builder()
							.withCode(code)
							.withStatus(status)
							.withStarted(start)
							.withEnded(end)
							.withDuration(calculateDuration(start, end))
							.withMessage(formatMessage(message, args))
							.build());

			return this;
		}

		@Override
		public DefaultMonitorHistory stepSucceeded(final Step step, final Date start, final Date end, final String message, final Object... args)
		{
			return add(step, Status.SUCCESS, start, end, null, message, args);
		}

		@Override
		public DefaultMonitorHistory stepFailed(final Step step, final Date start, final Date end, final Throwable ex, final String message, final Object... args)
		{
			return add(step, Status.FAILURE, start, end, ex, message, args);
		}

		private DefaultMonitorHistory add(final Step step, final Status status, final Date start, final Date end, final Throwable ex, final String message, final Object... args)
		{
			final MonitorHistoryData hist = getHistory();
			hist.setException(ex);
			hist.setExceptionMessage(ex == null ? null : ex.getMessage());
			hist.getEntries().add(
					MonitorHistoryEntryData.builder()
							.withStep(step)
							.withStatus(Status.SUCCESS.equals(status) && haveFailedActions(this.actions) ? Status.WARNING : status)
							.withStarted(start)
							.withEnded(end)
							.withDuration(calculateDuration(start, end))
							.withMessage(formatMessage(message, args))
							.withNodeId(getClusterId())
							.withActions(new LinkedList<>(this.actions))
							.build());
			this.actions.clear();
			return this;
		}

		private Long calculateDuration(final Date start, final Date end)
		{
			return start == null || end == null ? null : end.getTime() - start.getTime();
		}

		private boolean haveFailedActions(final List<MonitorHistoryEntryActionData> actions)
		{
			return actions.stream().map(MonitorHistoryEntryActionData::getStatus).anyMatch(s -> (Status.FAILURE.equals(s) || Status.WARNING.equals(s)));
		}

		@Override
		public void checkpoint()
		{
			getMonitorRecorder().record(getHistory());
			monitorHistory.remove();
		}

		@Override
		public void end(final Status status)
		{
			final MonitorHistoryData hist = getHistory();
			hist.setStatus(Status.SUCCESS.equals(status) && haveUnsuccessfulEntries(hist.getEntries()) ? Status.WARNING : status);
			getMonitorRecorder().record(hist);
			monitorHistory.remove();
		}

		private boolean haveUnsuccessfulEntries(final List<MonitorHistoryEntryData> actions)
		{
			return actions.stream().map(MonitorHistoryEntryData::getStatus).anyMatch(s -> (Status.FAILURE.equals(s) || Status.WARNING.equals(s)));
		}

		protected MonitorHistoryData getHistory()
		{
			return this.history;
		}
	}
}
