package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.service.ServiceRegistry;
import com.bupt.tarecruit.util.SceneNavigator;

/**
 * Common wiring logic for all controllers.
 */
public abstract class BaseController {

    protected SceneNavigator navigator;
    protected ServiceRegistry services;

    public void init(SceneNavigator navigator, ServiceRegistry services) {
        this.navigator = navigator;
        this.services = services;
        onInit();
    }

    protected void onInit() {
        // hook for subclasses
    }
}
