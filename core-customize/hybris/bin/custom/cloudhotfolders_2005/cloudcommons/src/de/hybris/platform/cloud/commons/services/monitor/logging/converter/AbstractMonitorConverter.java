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
package de.hybris.platform.cloud.commons.services.monitor.logging.converter;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.convert.converter.Converter;

public abstract class AbstractMonitorConverter<S,T> implements Converter<S,T>
{
	private DateTimeFormatter dateTimeFormatter;

	protected String formatMessage(final String message, final Object... params)
	{
		return MessageFormatter.arrayFormat(message, params).getMessage();
	}

	protected String formatDate(final Date date)
	{
		return date == null ? null : getDateTimeFormatter().print(new DateTime(date));
	}

	protected DateTimeFormatter getDateTimeFormatter()
	{
		return dateTimeFormatter;
	}

	@Required
	public void setDateTimeFormatter(final DateTimeFormatter dateTimeFormatter)
	{
		this.dateTimeFormatter = dateTimeFormatter;
	}
}
