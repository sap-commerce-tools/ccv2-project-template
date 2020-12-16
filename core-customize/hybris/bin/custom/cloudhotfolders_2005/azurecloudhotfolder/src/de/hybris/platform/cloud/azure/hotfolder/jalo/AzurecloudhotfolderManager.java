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
package de.hybris.platform.cloud.azure.hotfolder.jalo;

import de.hybris.platform.cloud.azure.hotfolder.constants.AzurecloudhotfolderConstants;
import de.hybris.platform.core.Registry;

/**
 * This is the extension manager of the Azurecloudhotfolder extension.
 */
public class AzurecloudhotfolderManager extends GeneratedAzurecloudhotfolderManager
{
	/**
	 * Get the valid instance of this manager.
	 *
	 * @return the current instance of this manager
	 */
	public static AzurecloudhotfolderManager getInstance()
	{
		return (AzurecloudhotfolderManager) Registry.getCurrentTenant().getJaloConnection().getExtensionManager()
				.getExtension(AzurecloudhotfolderConstants.EXTENSIONNAME);
	}
}
