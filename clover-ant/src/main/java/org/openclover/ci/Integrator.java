package org.openclover.ci;

import java.util.List;

/**
 * An Integrator is designed to provied a simple means to enable Clover for a build from a CI environment.
 * This interface also contains a Factory for creating new instances of implementing Integrator classes.
 */
public interface Integrator {
    /**
     * Takes a modifiable List of arguments, and decorates them with arguments that will enable Clover for the build.
     *
     * @param args a modifiable List of arguments to decorate.
     */
    void decorateArguments(List<String> args);

    class Factory {

        /**
         * Creates an integrator for Ant, using the given options.
         *
         * @param options the options the Integrator should use
         *
         * @return an Integrator to be used with Ant 1.7+ configured with the given options.
         */
        public static Integrator newAntIntegrator(CIOptions options) {
            return new AntIntegrator(options);
        }

        /**
         * Creates an integrator for Maven, using the given options.
         *
         * @param options the options the Integrator should use
         * @return an Integrator to be used with Maven2 configured with the given options.
         */
        public static Integrator newMavenIntegrator(CIOptions options) {
            return new MavenIntegrator(options);
        }
    }
}
