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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip;

/**
 * General task for execution in a ZIP batch pipeline.
 */
public interface ZipHeaderTask
{
	/**
	 * Executes a zip task with a predefined {@link ZipBatchHeader} identifying all relevant process information.
	 * @param header
	 * @return the header
	 */
	ZipBatchHeader executeZip(final ZipBatchHeader header);

}
