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
package de.hybris.platform.cloud.hotfolder.spring.integration.zip;

import org.springframework.integration.zip.ZipHeaders;

/**
 * Extended Zip adapter specific message headers.
 **/
public class CloudZipHeaders extends ZipHeaders
{
    public static final String PREFIX = ZipHeaders.PREFIX;
    public static final String ARCHIVE_PATH = PREFIX + "archivePath";
    public static final String UNZIP_DIRECTORY = PREFIX + "unzipDirectory";

    public CloudZipHeaders() {
        // empty to prevent instantiation
    }
}
