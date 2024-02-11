package org.openclover.runtime.remote;

import org.openclover.runtime.Logger;

import java.lang.reflect.InvocationTargetException;

/**
 */
public class RemoteFactory implements RemoteServiceProvider {


    private static final RemoteFactory INSTANCE = new RemoteFactory();

    public static RemoteFactory getInstance() {
        return INSTANCE;
    }

    private RemoteFactory() {

    }

    @Override
    public RecorderService createService(Config config) {
        final String className = "org.openclover.runtime.remote.CajoTcpRecorderService";
        
        Logger.getInstance().verbose("Creating service " + className + " for config: " + config.getName());
        final RecorderService service = (RecorderService) instantiate(className);
        service.init(config);
        return service;
    }

    @Override
    public RecorderListener createListener(Config config) {
        final String className = "org.openclover.runtime.remote.CajoTcpRecorderListener";
        Logger.getInstance().verbose("Creating listener " + className + "  for config: " + config.getName());
        final RecorderListener listener =  (RecorderListener) instantiate(className);
        listener.init(config);
        return listener;
    }

    @Override
    public Config createConfig(String serverLocation) {
        return new DistributedConfig(serverLocation);
    }

    private static Object instantiate(String className) {
   
        try {
            final Class clazz = Class.forName(className);
            return instantiate(clazz);
        } catch (ClassNotFoundException e) {
            Logger.getInstance().error("Could not load class: " + className, e);
        }
        return null;
    }

    private static Object instantiate(Class clazz) {

        if (clazz == null) {
            throw new IllegalArgumentException("Can not instantiate a null class.");
        }

        try {
            return clazz.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            Logger.getInstance().error("Could not create: " + clazz, e);
        }
        return null;
    }
    
}
