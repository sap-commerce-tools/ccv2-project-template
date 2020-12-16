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
package de.hybris.platform.cloud.commons.spring.integration.metadata.dao.impl;

import de.hybris.platform.cloud.commons.model.MetadataStoreEntryModel;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;

/**
 * {@link GenericDao} implementation to query for {@link MetadataStoreEntryModel}
 */
public class MetadataStoreEntryDao extends DefaultGenericDao<MetadataStoreEntryModel>
{
	public MetadataStoreEntryDao()
	{
		super(MetadataStoreEntryModel._TYPECODE);
	}
}
