/*
 * [y] hybris Platform
 *
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.cloud.hotfolder.spring.integration.file;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.messaging.Message;

/**
 * Extension of {@link DefaultFileNameGenerator} that appends a timestamp to the
 * end of the filename.
 */
public class TimestampFileNameGenerator extends DefaultFileNameGenerator
{
	@Override
	public String generateFileName(final Message<?> message)
	{
		final DateTime dt = new DateTime(DateTimeZone.UTC);
		final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		final String timestamp = fmt.print(dt).replace(":", "-");
		return super.generateFileName(message) + "." + timestamp;
	}
}
