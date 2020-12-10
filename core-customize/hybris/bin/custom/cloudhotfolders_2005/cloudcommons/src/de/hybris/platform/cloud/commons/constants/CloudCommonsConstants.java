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
package de.hybris.platform.cloud.commons.constants;

/**
 * Global class for all CloudCommons constants. You can add global constants for your extension into this class.
 */
public final class CloudCommonsConstants extends GeneratedCloudCommonsConstants
{
	public static final String EXTENSIONNAME = "cloudcommons";

	private CloudCommonsConstants()
	{
		//empty to avoid instantiating this constant class
	}

	public static final String MONITORING_METRIC_COUNTER_ENABLED = "cloud.commons.monitoring.metric.counter.enabled";
	public static final String MONITORING_METRIC_HISTOGRAM_ENABLED = "cloud.commons.monitoring.metric.histogram.enabled";
	public static final String MONITORING_METRIC_TIMER_ENABLED = "cloud.commons.monitoring.metric.timer.enabled";
	public static final String MONITORING_LOGGING_ENABLED = "cloud.commons.monitoring.logging.enabled";
	public static final String MONITORING_DB_ENABLED = "cloud.commons.monitoring.database.enabled";
}
