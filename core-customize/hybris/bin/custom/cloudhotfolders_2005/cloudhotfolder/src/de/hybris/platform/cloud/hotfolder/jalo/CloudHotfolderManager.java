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
package de.hybris.platform.cloud.hotfolder.jalo;

import de.hybris.platform.cloud.hotfolder.constants.CloudHotfolderConstants;
import de.hybris.platform.core.Registry;


/**
 * This is the extension manager of the CloudHotfolder extension.
 */
public class CloudHotfolderManager extends GeneratedCloudHotfolderManager
{
	/**
	 * Get the valid instance of this manager.
	 *
	 * @return the current instance of this manager
	 */
	public static CloudHotfolderManager getInstance()
	{
		return (CloudHotfolderManager) Registry.getCurrentTenant().getJaloConnection().getExtensionManager()
				.getExtension(CloudHotfolderConstants.EXTENSIONNAME);
	}
}
