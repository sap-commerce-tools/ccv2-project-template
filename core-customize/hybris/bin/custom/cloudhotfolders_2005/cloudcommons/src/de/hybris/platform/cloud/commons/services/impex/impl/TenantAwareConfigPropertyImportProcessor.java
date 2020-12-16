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
package de.hybris.platform.cloud.commons.services.impex.impl;

import de.hybris.platform.commerceservices.impex.impl.ConfigPropertyImportProcessor;
import de.hybris.platform.core.Registry;
import de.hybris.platform.impex.jalo.ImpExReader;
import de.hybris.platform.impex.jalo.imp.ImpExImportReader;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * @inheritDoc Allows substitution of ${tenantId} property where required
 */
public class TenantAwareConfigPropertyImportProcessor extends ConfigPropertyImportProcessor
{
	private static final String PROPERTY_KEY = "tenantId";
	private static final String SUBSTITUTE_KEY = "${tenantId}";

	private String tenantId;

	@Override
	public void init(final ImpExImportReader reader)
	{
		this.tenantId = Registry.getCurrentTenant().getTenantID();
		Assert.notNull(this.tenantId, "'tenantId' cannot be null");
		super.init(reader);
		addDefinition(reader, findAddDefinitionMethod(), "config-" + PROPERTY_KEY, this.tenantId);
	}

	protected void addDefinition(final ImpExReader reader, final Method addDefinitionMethod, final String key, final String value)
	{
		if (StringUtils.contains(value, SUBSTITUTE_KEY))
		{
			super.addDefinition(reader, addDefinitionMethod, key, StringUtils.replace(value, SUBSTITUTE_KEY, tenantId));
		}
		else
		{
			super.addDefinition(reader, addDefinitionMethod, key, value);
		}

	}
}
