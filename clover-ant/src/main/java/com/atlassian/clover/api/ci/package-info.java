/**
 *
 * Provides classes to programtically integrate Clover into an Ant or Maven2 build, typically from
 * within a Continuous Integration server.
 *
 * Clover can be integrated into both Ant and Maven2 purely by decorating the respective command lines.
 * This api adds a nice wrapper around creating the appropriate list of arguments to add to the command line depending
 * on what options an end user has configured.
 * <p>&nbsp;</p>
 * The {@link com.atlassian.clover.api.ci.CIOptions.Builder} should be used to create and configure a
 * {@link com.atlassian.clover.api.ci.CIOptions} instance that then gets passed to one of the
 * {@link com.atlassian.clover.api.ci.Integrator.Factory}'s "new*" methods.
 *
 * <h2>Examples</h2>
 * To integrate Clover into an Ant build, with the default set of options you would use:
 * <pre>
 *  CIOptions options =  new CIOptions.Builder().build();
 *  Integrator antIntegrator = Integrator.Factory.newAntIntegrator(options);
 *  List&lt;String&gt; args = new ArrayList&lt;String&gt;(Arrays.asList("clean","test"));
 *  antIntegrator.decorateArguments(args);
 *
 *  // args now contain extra args that will enable Clover to instrument all java source files and generate a coverage report.
 *
 * </pre>
 *
 *
 * <h2>Related Documentation</h2>
 *
 * For more information relating to Clover integration, please view the
 * <ul>
 * <li><a href="http://confluence.atlassian.com/x/FIREB">Clover for Ant User Guide</a>
 * <li><a href="http://confluence.atlassian.com/x/K4CDBQ">Clover for Maven2 User Guide</a>
 * </ul>
 *
 * If you have any problems using this api, or feature requests please don't hesitate to
 * <a href="http://jira.atlassian.com/browse/CLOV">create a JIRA issue</a>.
 * @since 2.6.0
 */
package com.atlassian.clover.api.ci;