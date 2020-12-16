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
package de.hybris.platform.cloud.commons.spring.integration.support.locks.database.dao.impl;

import de.hybris.platform.cloud.commons.model.ApplicationResourceLockModel;
import de.hybris.platform.cloud.commons.spring.integration.support.locks.database.dao.ApplicationResourceLockDao;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link ApplicationResourceLockDao} to allow for querying for instances of
 * {@link ApplicationResourceLockModel}
 */
public class DefaultApplicationResourceLockDao extends DefaultGenericDao<ApplicationResourceLockModel>
        implements ApplicationResourceLockDao
{

    private static final String FIND_ACQUIRED_LOCK = "SELECT {" + ApplicationResourceLockModel.PK + "} FROM {"
            + ApplicationResourceLockModel._TYPECODE + "} WHERE {" + ApplicationResourceLockModel.LOCKKEY + "} = ?lockKey AND {"
            + ApplicationResourceLockModel.REGION + "} = ?region AND {" + ApplicationResourceLockModel.CLUSTERID +
            "} = ?clusterId AND {"
            + ApplicationResourceLockModel.TIMESTAMP + "} >= ?expiryDate";

    private static final String FIND_LOCKS_BY_EXPIRY = "SELECT {" + ApplicationResourceLockModel.PK + "} FROM {"
            + ApplicationResourceLockModel._TYPECODE + "} WHERE {" + ApplicationResourceLockModel.LOCKKEY + "} = ?lockKey AND {"
            + ApplicationResourceLockModel.REGION + "} = ?region AND {" + ApplicationResourceLockModel.TIMESTAMP +
            "} < ?expiryDate";

    public DefaultApplicationResourceLockDao()
    {
        this(ApplicationResourceLockModel._TYPECODE);
    }

    public DefaultApplicationResourceLockDao(final String typecode)
    {
        super(typecode);
    }

    /**
     * @inheritDoc
     */
    @Override
    public ApplicationResourceLockModel getLock(final String key, final String region, final int clusterId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put(ApplicationResourceLockModel.LOCKKEY, key);
        params.put(ApplicationResourceLockModel.REGION, region);
        params.put(ApplicationResourceLockModel.CLUSTERID, clusterId);
        final List<ApplicationResourceLockModel> results = find(params);
        if (!results.isEmpty())
        {
            return results.get(0);
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public ApplicationResourceLockModel getLock(final String key,
                                                final String region,
                                                final int clusterId,
                                                final Date expiryDate)
    {
        final FlexibleSearchQuery query = buildQuery(FIND_ACQUIRED_LOCK, key, region, expiryDate);
        query.addQueryParameter("clusterId", clusterId);
        final SearchResult<ApplicationResourceLockModel> result = getFlexibleSearchService().search(query);
        final List<ApplicationResourceLockModel> results = result.getResult();
        if (!results.isEmpty())
        {
            return results.get(0);
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<ApplicationResourceLockModel> getLocks(final String region, final int clusterId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put(ApplicationResourceLockModel.REGION, region);
        params.put(ApplicationResourceLockModel.CLUSTERID, clusterId);
        return find(params);
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<ApplicationResourceLockModel> getLocks(final String key, final String region, final Date expiryDate)
    {
        final FlexibleSearchQuery query = buildQuery(FIND_LOCKS_BY_EXPIRY, key, region, expiryDate);
        final SearchResult<ApplicationResourceLockModel> result = getFlexibleSearchService().search(query);
        return result.getResult();
    }

    protected FlexibleSearchQuery buildQuery(final String query, final String key, final String region, final Date expiryDate)
    {
        final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(query);
        flexibleSearchQuery.addQueryParameter("lockKey", key);
        flexibleSearchQuery.addQueryParameter("region", region);
        flexibleSearchQuery.addQueryParameter("expiryDate", expiryDate);
        return flexibleSearchQuery;
    }

}
