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
package de.hybris.platform.cloud.commons.services.monitor;

import java.util.Optional;

/**
 * Service to add to/begin/resume history information for areas of the running system
 */
public interface MonitorService
{
	/**
	 * Begin new history
	 * @param area area of the system being monitored
	 * @param key the unique key for the record
	 * @return {@link MonitorHistory} to encapsulate information captured
	 */
	MonitorHistory begin(final SystemArea area, final String key);

	/**
	 * Return a handle on the {@link MonitorHistory} for the current thread
	 * @return
	 */
	Optional<MonitorHistory> current();

	/**
	 * Resume monitoring
	 * @param area area of the system being monitored
	 * @param key the unique key for the record
	 * @return {@link MonitorHistory} containing all the previous entries, if Area/Key combination found.  Else empty {@link MonitorHistory}
	 */
	MonitorHistory resume(final SystemArea area, final String key);
}
