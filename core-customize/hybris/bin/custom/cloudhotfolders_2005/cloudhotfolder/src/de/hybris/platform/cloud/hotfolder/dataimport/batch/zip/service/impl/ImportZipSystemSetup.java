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
package de.hybris.platform.cloud.hotfolder.dataimport.batch.zip.service.impl;

import de.hybris.platform.commerceservices.setup.AbstractSystemSetup;
import de.hybris.platform.core.initialization.SystemSetupParameter;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides hooks into the system's initialization and update processes.
 */
public class ImportZipSystemSetup extends AbstractSystemSetup
{
	private List<String> setupParameterKeys;

	@Override
	public List<SystemSetupParameter> getInitializationOptions()
	{
		return getSetupParameterKeys().stream()
				.map(s -> createBooleanSystemSetupParameter(s, s, true))
				.collect(Collectors.toList());
	}

	protected List<String> getSetupParameterKeys()
	{
		return setupParameterKeys;
	}

	@Required
	public void setSetupParameterKeys(final List<String> setupParameterKeys)
	{
		this.setupParameterKeys = setupParameterKeys;
	}

}
