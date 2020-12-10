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
package de.hybris.platform.cloud.azure.hotfolder.remote.session;

import org.springframework.integration.file.remote.session.Session;

import java.io.IOException;

/**
 * Extended abstraction for a Session with a remote File system.
 *
 */
public interface ExtendedSession<F> extends Session<F>
{
	/**
	 * Get a direct handle on the remote object
	 * @param path the remote path.
	 * @return F the remote object
	 * @throws IOException an IO Exception.
	 */
	F get(final String path) throws IOException;
}
