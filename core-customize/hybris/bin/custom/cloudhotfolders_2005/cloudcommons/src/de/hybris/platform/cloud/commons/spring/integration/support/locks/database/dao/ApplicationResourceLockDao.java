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
package de.hybris.platform.cloud.commons.spring.integration.support.locks.database.dao;

import de.hybris.platform.cloud.commons.model.ApplicationResourceLockModel;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;

import java.util.Date;
import java.util.List;

/**
 * Extended implementation of {@link GenericDao} to allow for querying for instances of
 * {@link ApplicationResourceLockModel}
 */
public interface ApplicationResourceLockDao extends GenericDao<ApplicationResourceLockModel>
{
	/**
	 * Get Lock based on the parameters
	 * @param key       value of model
	 * @param region    value of model
	 * @param clusterId value of model
	 * @return matching model, or null
	 */
	ApplicationResourceLockModel getLock(final String key, final String region, final int clusterId);

	/**
	 * Get Lock based on the parameters
	 * @param key       value of model
	 * @param region    value of model
	 * @param clusterId value of model
	 * @param expiryDate where is less than model timestamps
	 * @return matching model, or null
	 */
	ApplicationResourceLockModel getLock(final String key, final String region, int clusterId, final Date expiryDate);

	/**
	 * Get Locks based on the parameters
	 * @param region    value of model
	 * @param clusterId value of model
	 * @return matching models
	 */
	List<ApplicationResourceLockModel> getLocks(final String region, final int clusterId);

	/**
	 * Get Locks based on the parameters
	 * @param key    value of model
	 * @param region value of model
	 * @param expiryDate where is less than model timestamps
	 * @return matching models
	 */
	List<ApplicationResourceLockModel> getLocks(final String key, final String region, final Date expiryDate);

}
