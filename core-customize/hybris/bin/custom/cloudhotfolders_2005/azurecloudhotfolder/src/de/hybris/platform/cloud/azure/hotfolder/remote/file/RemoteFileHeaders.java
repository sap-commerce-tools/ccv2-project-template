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
package de.hybris.platform.cloud.azure.hotfolder.remote.file;

import de.hybris.platform.cloud.hotfolder.spring.integration.file.CloudFileHeaders;

/**
 * Pre-defined remote header names to be used when storing or retrieving
 * File-related values to/from integration Message Headers.
 *
 **/
public class RemoteFileHeaders extends CloudFileHeaders
{

    private static final String PREFIX = "file_";

    public static final String REMOTE_FILE_DELETED = PREFIX + "remoteFileDeleted";

}
