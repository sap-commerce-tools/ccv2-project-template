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
package de.hybris.platform.cloud.commons.spring.integration.metadata;

import com.google.common.cache.CacheBuilder;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataStore;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Simple implementation of {@link MetadataStore} that uses a {@link com.google.common.cache.Cache} for the data store.
 * The metadata will not be persisted between application restarts and will be evicted once the ttl expires or the max
 * size is reached.
 */
public class ExpiringSimpleMetadataStore implements ConcurrentMetadataStore
{

    private final ConcurrentMap<Object, Object> metadata;

    public ExpiringSimpleMetadataStore(final long maxSize, final long ttl)
    {
        this.metadata = CacheBuilder
                .newBuilder()
                .concurrencyLevel(1)
                .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
                .maximumSize(maxSize)
                .build()
                .asMap();
    }

    @Override
    public void put(final String key, final String value)
    {
        this.metadata.put(key, value);
    }

    @Override
    public String get(final String key)
    {
        return (String) this.metadata.get(key);
    }

    @Override
    public String remove(final String key)
    {
        return (String) this.metadata.remove(key);
    }

    @Override
    public String putIfAbsent(final String key, final String value)
    {
        return (String) this.metadata.putIfAbsent(key, value);
    }

    @Override
    public boolean replace(final String key, final String oldValue, final String newValue)
    {
        return this.metadata.replace(key, oldValue, newValue);
    }

}
