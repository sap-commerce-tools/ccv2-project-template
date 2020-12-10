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

import de.hybris.bootstrap.annotations.UnitTest;

import java.io.File;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
public class ZipBatchHeaderTest
{
	private ZipBatchHeader header = new ZipBatchHeader();
	private File parent = new File("parent");
	private File child = new File(parent, "dummyLocation");

	@Test
	public void unzippedFolderShouldBeRetainedAndSetAsTransformedFiles()
	{
		header.setFileUnzippedAs(child);
		assertThat(header.getFileUnzippedAs()).isEqualTo(child);
		assertThat(header.getFile()).isEqualTo(child);
		assertThat(header.getUnzippedFolder()).isEqualTo(child.getParentFile());
	}

}