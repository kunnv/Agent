/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.local_api;

import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.HashMap;
import java.util.Map;

/**
 * Local api point of start using iofog.
 * Get and update the configuration for local api module.
 * @author ashita
 * @since 2016
 */
public class LocalApi implements Runnable {

	private final String MODULE_NAME = "Local API";
	private static volatile LocalApi instance;
	private LocalApiServer server;

	private LocalApi() {

	}

	/**
	 * Instantiate local api - singleton
	 * @return LocalApi
	 */
	public static LocalApi getInstance(){
		LocalApi localInstance = instance;
		if (localInstance == null) {
			synchronized (LocalApi.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new LocalApi();
					LoggingService.logInfo("LOCAL API ","Local Api Instantiated");
				}
			}
		}
		return localInstance;
	}

	/**
	 * Stop local api server
	 */
	public void stopServer() {
		server.stop();
	}


	/**
	 * Start local api server
	 * Instantiate websocket map and configuration map
	 */
	@Override
	public void run() {
		StatusReporter.setSupervisorStatus().setModuleStatus(Constants.LOCAL_API, ModulesStatus.STARTING);

		StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.controlWebsocketMap.size());
		StatusReporter.setLocalApiStatus().setOpenMessageSocketsCount(WebSocketMap.messageWebsocketMap.size());

		retrieveContainerConfig();

		server = new LocalApiServer();
		try {
			server.start();
		} catch (Exception e) {
				stopServer();
				LoggingService.logWarning(MODULE_NAME, "unable to start local api server: " + e.getMessage());
				StatusReporter.setSupervisorStatus().setModuleStatus(Constants.LOCAL_API, ModulesStatus.STOPPED);
		}

	}

	/**
	 * Get the containers configuration and store it.
	 */
	private void retrieveContainerConfig() {
			ConfigurationMap.containerConfigMap = ElementManager.getInstance().getConfigs();
			LoggingService.logInfo(MODULE_NAME, "Container configuration retrieved");
	}

	/**
	 * Update the containers configuration and store it.
	 */
	private void updateContainerConfig() {
		ConfigurationMap.containerConfigMap = ElementManager.getInstance().getConfigs();
		LoggingService.logInfo(MODULE_NAME, "Container configuration updated");
	}

	/**
	 * Initiate the real-time control signal when the cofiguration changes.
	 * Called by field-agtent.
	 */
	public void update(){
		Map<String, String> oldConfigMap = new HashMap<>();
		oldConfigMap.putAll(ConfigurationMap.containerConfigMap);
		updateContainerConfig();
		Map<String, String> newConfigMap = new HashMap<>();
		ConfigurationMap.containerConfigMap.putAll(newConfigMap);
		ControlWebsocketHandler handler = new ControlWebsocketHandler();
		handler.initiateControlSignal(oldConfigMap, newConfigMap);
	}
}