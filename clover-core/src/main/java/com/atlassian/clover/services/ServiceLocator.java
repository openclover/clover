package com.atlassian.clover.services;

import org.openclover.runtime.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static org.openclover.util.Lists.newLinkedList;

/**
 * Locates service implementations via the META-INF services lookup protocol.
 *
 * @param <S> The type of service to look up
 */
public class ServiceLocator<S> implements Iterable<S> {
    private final Class<S> spi;
    private final List<URL> providers;

    private ServiceLocator(Class<S> spi, List<URL> providers) {
        this.spi = spi;
        this.providers = providers;
    }

    /**
     * Note: the iterator may throw the unchecked exception ServiceNotAvailableException on next() if there was a problem instantiating the service.
     * @return an iterator on the service implementations
     */
    @Override
    public Iterator<S> iterator() {
        final Iterator<URL> providerIterator = providers.iterator();

        return new Iterator<S>() {
            @Override
            public boolean hasNext() {
                return providerIterator.hasNext();
            }

            @Override
            public S next() {
                URL provider = providerIterator.next();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(provider.openStream()));
                    String className = reader.readLine().trim();
                    try {
                        return (S) Class.forName(className).getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new ServiceNotAvailableException("Failed to instantiate service provider " + className, e);
                    }
                } catch (IOException e) {
                    throw new ServiceNotAvailableException("Failed to instantiate service provider for resource " + provider, e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * @param spi the interface or abstract base class of services to find
     * @param classLoader used to load /META-INF/services/service.class.name - the thread context classloader is used if one is not supplied
     * @param <S> the type of the service
     * @return a ServiceLocator instance used to iterate over the currently available services
     */
    public static <S> ServiceLocator<S> load(Class<S> spi, ClassLoader classLoader) {
        List<URL> providers = newLinkedList();

        String serviceResource = "/META-INF/services/" + spi.getName();
        try {
            Enumeration<URL> resources = (classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader).getResources(serviceResource);
            while (resources.hasMoreElements()) {
                providers.add(resources.nextElement());
            }
        } catch (Exception e) {
            Logger.getInstance().warn("Failed to load service resources " + serviceResource, e);
        }

        return new ServiceLocator(spi, providers);
    }
}
