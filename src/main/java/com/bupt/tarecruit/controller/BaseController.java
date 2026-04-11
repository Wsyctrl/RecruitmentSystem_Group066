package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.service.ServiceRegistry;
import com.bupt.tarecruit.util.SceneNavigator;

/**
 * Base class for all JavaFX controllers in the application.
 * It provides shared dependencies such as scene navigation
 * and access to application services.
 */
public abstract class BaseController {

    /**
     * Navigator used to switch scenes and access the primary stage.
     */
    protected SceneNavigator navigator;

    /**
     * Registry that provides access to shared application services.
     */
    protected ServiceRegistry services;

    /**
     * Initializes shared dependencies for the controller.
     * This method should be called after the controller is created
     * so that subclasses can use navigation and service objects.
     *
     * @param navigator navigator used for scene transitions
     * @param services registry containing application services
     */
    public void init(SceneNavigator navigator, ServiceRegistry services) {
        this.navigator = navigator;
        this.services = services;
        onInit();
    }

    /**
     * Hook method for subclasses to perform additional initialization
     * after shared dependencies have been injected.
     */
    protected void onInit() {
        // hook for subclasses
    }
}