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
package de.hybris.platform.cloud.commons.spring.integration.support.locks.database;

import de.hybris.platform.cloud.commons.model.ApplicationResourceLockModel;
import de.hybris.platform.cloud.commons.spring.context.SmartLifecycleRole;
import de.hybris.platform.cloud.commons.spring.integration.support.locks.database.dao.ApplicationResourceLockDao;
import de.hybris.platform.core.Registry;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.jdbc.lock.LockRepository;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link LockRepository} using the Hybris Type system.
 * Also implements {@link SmartLifecycleRole} to ensure locks created by the running node are deleted upon shutdown.
 */
public class HybrisLockRepository implements SmartLifecycleRole, LockRepository, BeanNameAware
{
	private static final Logger LOG = LoggerFactory.getLogger(HybrisLockRepository.class);

	private static final String DEFAULT_REGION = "Default";
	private static final int DEFAULT_TTL = 10000;

	private final Object lifecycleMonitor = new Object();
	private final Object deletingMonitor = new Object();

	private volatile boolean autoStartup = true;
	private volatile int phase = 0;
	private volatile String role = null;
	private volatile boolean running;

	private String region = DEFAULT_REGION;
	private int ttl = DEFAULT_TTL;

	private ModelService modelService;
	private ApplicationResourceLockDao applicationResourceLockDao;
	private String beanName;

	@Override
	public void start()
	{
		synchronized (this.lifecycleMonitor)
		{
			this.running = true;
		}
	}

	@Override
	public void stop()
	{
		synchronized (this.lifecycleMonitor)
		{
			if (this.isRunning())
			{
				LOG.info("Deleting all lock records in region [{}] for node [{}].", getRegion(), getClusterId());
				synchronized (this.deletingMonitor)
				{
					final List<ApplicationResourceLockModel> locks
							= getApplicationResourceLockDao().getLocks(getRegion(), getClusterId());
					deleteLocks(locks);
				}
				this.running = false;
			}
		}
	}

	@Override
	public String getRole()
	{
		return this.role;
	}

	public void setRole(final String role)
	{
		this.role = role;
	}

	@Override
	public boolean isAutoStartup()
	{
		return this.autoStartup;
	}

	public void setAutoStartup(final boolean autoStartup)
	{
		this.autoStartup = autoStartup;
	}

	@Override
	public int getPhase()
	{
		return this.phase;
	}

	public void setPhase(final int phase)
	{
		this.phase = phase;
	}

	@Override
	public void stop(final Runnable runnable)
	{
		stop();
		runnable.run();
	}

	@Override
	public boolean isRunning()
	{
		return this.running;
	}

	@Override
	public boolean acquire(final String key)
	{
		LOG.debug("Acquiring lock for key [{}] in region [{}] for node [{}].", key, getRegion(), getClusterId());
		try
		{
			modelService.enableTransactions();
			deleteExpiredLocks(key);
			try
			{
				final ApplicationResourceLockModel lock =
						Optional.ofNullable(getApplicationResourceLockDao().getLock(key, getRegion(), getClusterId()))
								.orElseGet(() -> createNewLock(key, getRegion(), getClusterId()));

				final Date timestamp = Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
				lock.setTimestamp(timestamp);

				logLockStatus(lock);
				return saveLock(lock);
			}
			finally
			{
				modelService.clearTransactionsSettings();
			}

		}
		catch (final RuntimeException ex)
		{
			LOG.debug("Unable to create or update record for lock for key [{}] in region [{}] for node [{}].",
					key, getRegion(), getClusterId(), ex);
			return false;
		}
	}

	@Override
	public void close()
	{
		// Close is handled by the lifecycle stop event.
	}

	@Override
	public boolean isAcquired(final String key)
	{
		deleteExpiredLocks(key);
		final Date expiryDate = calculateExpiryDate(getTtl());
		final ApplicationResourceLockModel lock = getApplicationResourceLockDao().getLock(key, getRegion(), getClusterId(), expiryDate);
		if (lock == null)
		{
			LOG.debug("Unable to find lock for key [{}] in region [{}] for node [{}].", key, getRegion(), getClusterId());
			return false;
		}
		else
		{
			LOG.debug("Lock {} found for key [{}].", toString(lock), key);
			return true;
		}
	}

	@Override
	public void delete(final String key)
	{
		LOG.debug("Releasing lock for key [{}] in region [{}] for node [{}].", key, getRegion(), getClusterId());
		synchronized (deletingMonitor)
		{
			final ApplicationResourceLockModel lock = getApplicationResourceLockDao().getLock(key, getRegion(), getClusterId());
			if (lock != null)
			{
				deleteLocks(Collections.singletonList(lock));
			}
			else
			{
				LOG.debug("Unable to remove application lock [{}].", key);
			}
		}
	}

	protected void logLockStatus(final ApplicationResourceLockModel lock)
	{
		if (LOG.isDebugEnabled())
		{
			if (modelService.isNew(lock))
			{
				LOG.debug("Creating record for lock {}.", HybrisLockRepository.toString(lock));
			}
			else
			{
				LOG.debug("Updating record for lock {}.", HybrisLockRepository.toString(lock));
			}
		}
	}

	protected static String toString(final ApplicationResourceLockModel lock)
	{
		return new ToStringBuilder(lock, ToStringStyle.NO_CLASS_NAME_STYLE)
				.append("lockKey", lock.getLockKey())
				.append("region", lock.getRegion())
				.append("clusterId", lock.getClusterId())
				.append("timestamp", lock.getTimestamp())
				.toString();
	}

	protected int getClusterId()
	{
		return Registry.getClusterID();
	}

	protected void deleteExpiredLocks(final String key)
	{
		final Date expiryDate = calculateExpiryDate(getTtl());

		LOG.debug("Deleting lock records for key [{}] in region [{}] older than [{}].",
				key, getRegion(), expiryDate);

		synchronized (deletingMonitor)
		{
			final List<ApplicationResourceLockModel> locks =
					getApplicationResourceLockDao().getLocks(key, getRegion(), expiryDate);
			deleteLocks(locks);
		}
	}

	protected Date calculateExpiryDate(final int ttl)
	{
		return Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant().minusMillis(ttl));
	}

	protected void deleteLocks(final List<ApplicationResourceLockModel> locks)
	{
		if (CollectionUtils.isNotEmpty(locks))
		{
			try
			{
				modelService.enableTransactions();
				// Not using ModelService.removeAll as it's all-or-nothing.
				// We want to remove as many locks as possible.
				for (final ApplicationResourceLockModel lock : locks)
				{
					deleteLock(lock);
				}
			}
			catch (final RuntimeException ex)
			{
				LOG.error("Unable to delete locks.", ex);
			}
			finally
			{
				modelService.clearTransactionsSettings();
			}
		}
	}

	protected boolean saveLock(final ApplicationResourceLockModel lock)
	{
		// Unique constraint on the the resource+scope columns will prevent
		// more than one caller obtaining a lock.
		try
		{
			modelService.save(lock);
			return true;
		}
		catch (final ModelSavingException ex)
		{
			LOG.debug("Unable to create or update record for lock for key [{}] in region [{}] for node [{}].",
					lock.getLockKey(), getRegion(), getClusterId());
			return false;
		}
	}

	protected void deleteLock(final ApplicationResourceLockModel lock)
	{
		final String key = HybrisLockRepository.toString(lock);
		LOG.debug("Deleting lock record {}.", key);
		if(modelService.isAttached(lock))
		{
			try
			{
				modelService.remove(lock);

			}
			catch (final RuntimeException ex)
			{
				LOG.debug("Unable to remove lock record {}.", key, ex);
			}
		}
		else
		{
			LOG.warn("Unable to delelete detatched lock [{}].", lock);
		}
	}

	protected ApplicationResourceLockModel createNewLock(final String key, final String region, final int clusterId)
	{
		final ApplicationResourceLockModel lock = getModelService().create(ApplicationResourceLockModel.class);
		lock.setLockKey(key);
		lock.setRegion(region);
		lock.setClusterId(clusterId);
		return lock;
	}

	protected ApplicationResourceLockDao getApplicationResourceLockDao()
	{
		return applicationResourceLockDao;
	}

	@Required
	public void setApplicationResourceLockDao(final ApplicationResourceLockDao applicationResourceLockDao)
	{
		this.applicationResourceLockDao = applicationResourceLockDao;
	}

	protected ModelService getModelService()
	{
		return modelService;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	protected String getRegion()
	{
		return region;
	}

	public void setRegion(final String region)
	{
		this.region = region;
	}

	protected int getTtl()
	{
		return ttl;
	}

	public void setTtl(final int ttl)
	{
		this.ttl = ttl;
	}

	@Override
	public void setBeanName(final String name)
	{
		this.beanName = name;
	}

	@Override
	public String toString()
	{
		return this.beanName;
	}
}
