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

import com.google.common.collect.ImmutableMap;
import de.hybris.platform.cloud.commons.model.MetadataStoreEntryModel;
import de.hybris.platform.core.Registry;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.concurrent.locks.Lock;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@link ConcurrentMetadataStore} using a Hybris Type system.
 * Could potentially replace this implementation once update to spring integration 5.0
 *
 * @see <a href="https://raw.githubusercontent.com/spring-projects/spring-integration/master/spring-integration-jdbc/src/main/java/org/springframework/integration/jdbc/metadata/JdbcMetadataStore.java">org.springframework.integration.jdbc.metadata.JdbcMetadataStore</a>
 * @see ConcurrentMetadataStore
 */
public class HybrisMetadataStore implements ConcurrentMetadataStore
{
    @SuppressWarnings("unused")
    private static final Logger LOG = getLogger(HybrisMetadataStore.class);

    private static final String KEY_CANNOT_BE_NULL = "'key' cannot be null";
    private static final String UNABLE_TO_ACQUIRE_LOCK = "Unable to acquire lock for key ";

    private final ModelService modelService;
    private final GenericDao<MetadataStoreEntryModel> metadataStoreDao;
    private final LockRegistry lockRegistry;
    private final String region;
    private final long lockTtl;

    public HybrisMetadataStore(final ModelService modelService,
                               final GenericDao<MetadataStoreEntryModel> metadataStoreDao,
                               final LockRegistry lockRegistry,
                               final long lockTtl,
                               final String region)
    {
        Assert.notNull(modelService, "modelService cannot be null");
        Assert.notNull(metadataStoreDao, "metadataStoreDao cannot be null");
        Assert.notNull(lockRegistry, "lockRegistry cannot be null");
        Assert.isTrue(StringUtils.isNotBlank(region), "region cannot be null or blank");
        this.modelService = modelService;
        this.metadataStoreDao = metadataStoreDao;
        this.lockRegistry = lockRegistry;
        this.lockTtl = lockTtl;
        this.region = region;
    }

    @Override
    public void put(final String key, final String value)
    {
        Assert.notNull(key, KEY_CANNOT_BE_NULL);
        Assert.notNull(value, "'value' cannot be null");
        final Lock lock = obtainLockObject(key);
        if (lock.tryLock())
        {
            try
            {
                createNewEntry(key, value);
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            throw new MessagingException(UNABLE_TO_ACQUIRE_LOCK + key);
        }
    }

    @Override
    public String putIfAbsent(final String key, final String value)
    {
        Assert.notNull(key, KEY_CANNOT_BE_NULL);
        Assert.notNull(value, "'value' cannot be null");
        final Lock lock = obtainLockObject(key);
        if (lock.tryLock())
        {
            try
            {
                return getCurrentEntry(key)
                        .map(MetadataStoreEntryModel::getValue)
                        .orElseGet(() -> {
                            createNewEntry(key, value);
                            /*
                             * @see {@link org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter#accept(java.lang.Object)}
                             * expects null where record was not in the store originally
                             */
                            return null;
                        });
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            throw new MessagingException(UNABLE_TO_ACQUIRE_LOCK + key);
        }
    }

    @Override
    public boolean replace(final String key, final String oldValue, final String newValue)
    {
        Assert.notNull(key, KEY_CANNOT_BE_NULL);
        Assert.notNull(oldValue, "'oldValue' cannot be null");
        Assert.notNull(newValue, "'newValue' cannot be null");
        final Lock lock = obtainLockObject(key);
        if (lock.tryLock())
        {
            try
            {
                return getCurrentEntry(key)
                        .filter(e -> StringUtils.equals(oldValue, e.getValue()))
                        .map(e -> {
                            LOG.debug("Update MetadataStoreEntry key [{}] oldValue [{}] newValue [{}]", key, oldValue, newValue);
                            e.setValue(newValue);
                            modelService.save(e);
                            return Boolean.TRUE;
                        })
                        .orElse(Boolean.FALSE);
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            throw new MessagingException(UNABLE_TO_ACQUIRE_LOCK + key);
        }
    }

    @Override
    public String get(final String key)
    {
        Assert.notNull(key, KEY_CANNOT_BE_NULL);
        final Lock lock = obtainLockObject(key);
        if (lock.tryLock())
        {
            try
            {
                return getCurrentEntry(key)
                        .map(MetadataStoreEntryModel::getValue)
                        .orElse(null);
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            throw new MessagingException(UNABLE_TO_ACQUIRE_LOCK + key);
        }
    }

    @Override
    public String remove(final String key)
    {
        Assert.notNull(key, KEY_CANNOT_BE_NULL);
        final Lock lock = obtainLockObject(key);
        if (lock.tryLock())
        {
            try
            {
                return getCurrentEntry(key)
                        .map(e -> {
                            LOG.debug("Removing MetadataStoreEntry for key [{}]", key);
                            final String value = e.getValue();
                            modelService.remove(e);
                            return value;
                        })
                        .orElse(null);
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            throw new MessagingException(UNABLE_TO_ACQUIRE_LOCK + key);
        }
    }

    private Lock obtainLockObject(final String key)
    {
        // Expire locks according to TTL.
        if (lockRegistry instanceof ExpirableLockRegistry)
        {
            ((ExpirableLockRegistry) lockRegistry).expireUnusedOlderThan(this.lockTtl);
        }
        return this.lockRegistry.obtain(key);
    }

    private void createNewEntry(final String key, final String value)
    {
        LOG.debug("Creating new MetadataStoreEntry for key [{}] with value [{}]", key, value);
        final MetadataStoreEntryModel entry = modelService.create(MetadataStoreEntryModel.class);
        entry.setRegion(region);
        entry.setKey(key);
        entry.setValue(value);
        modelService.save(entry);
    }

    private Optional<MetadataStoreEntryModel> getCurrentEntry(final String key)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Check for current entry for key [{}] in region [{}] for node [{}]", key, region, Registry.getClusterID());
        }

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put(MetadataStoreEntryModel.REGION, region)
                .put(MetadataStoreEntryModel.KEY, key)
                .build();

        LOG.debug("Querying for MetadataStoreEntry for region [{}] value [{}]", region, key);

        return Optional.ofNullable(metadataStoreDao.find(params))
                .filter(CollectionUtils::isNotEmpty)
                .map(s -> s.get(0));
    }

}
