package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.registry.metrics.BlockMetrics;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.metrics.ClassMetrics;
import com.atlassian.clover.registry.metrics.FileMetrics;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.metrics.PackageMetrics;
import com.atlassian.clover.registry.metrics.ProjectMetrics;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;

/**
 * Adapts IProject, IPackageFragment, ICompilationUnit and IType
 * to HasMetrics and BlockMetrics classes.
 */
 public class MetricsAdapterFactory implements IAdapterFactory {
     /**
      * We adapt other instances to these classes
      */
     private static final Class[] ADAPTED_TO_TYPES = {
         BlockMetrics.class, ClassMetrics.class,
         FileMetrics.class, PackageMetrics.class,
         ProjectMetrics.class, HasMetrics.class,
         FullProjectInfo.class, FullFileInfo.class,
         FullClassInfo.class, TestCaseInfo.class,
         TestCaseInfo[].class, FullMethodInfo.class
     };

     @Override
     public Class[] getAdapterList() {
         return ADAPTED_TO_TYPES;
     }


     @Override
     public Object getAdapter(Object object, Class clazz) {
         if (CloverPlugin.getInstance().isLicensePresent()
             && object instanceof MetricsScope.Qualification) {
             Object adapter =
                 getNaturalAdapter(
                     ((MetricsScope.Qualification)object).getAdaptable(),
                     ((MetricsScope.Qualification)object).getScope(),
                     clazz);
             //Filter out adapters that are more general than the one asked for
             return clazz.isInstance(adapter) ? adapter : null;
         } else {
             return null;
         }
     }

     public Object getNaturalAdapter(Object adaptee, MetricsScope scope, Class clazz) {
         try {
             if (HasMetrics.class.isAssignableFrom(clazz)
                 || BlockMetrics.class.isAssignableFrom(clazz)) {

                 if (adaptee instanceof IProject) {
                     return getNaturalAdapterOnProject((IProject)adaptee, adaptee, scope, clazz);
                 } else {
                     return getNaturalJavaElementAdapter(adaptee, scope, clazz);
                 }
             } else if (TestCaseInfo.class.isAssignableFrom(clazz) && adaptee instanceof IMethod) {
                 CloverProject cloverProject = CloverProject.getFor(((IMethod) adaptee).getJavaProject());
                 return cloverProject == null ? null : cloverProject.getModel().getTestCaseInfo((IMethod)adaptee, scope);
             } else if (clazz.isArray() && TestCaseInfo.class.isAssignableFrom(clazz.getComponentType()) && adaptee instanceof IMethod) {
                 CloverProject cloverProject = CloverProject.getFor(((IMethod) adaptee).getJavaProject());
                 return cloverProject == null ? null : cloverProject.getModel().getTestCaseInfos((IMethod)adaptee, scope);
             }
         } catch (Exception e) {
             CloverPlugin.logError("Unable to adapt " + adaptee + " to " + clazz.getName(), e);
         }
         return null;
     }

     private Object getNaturalAdapterOnProject(IProject project, Object adaptee, MetricsScope scope, Class clazz) throws CoreException {
         return getMetrics(CloverProject.getFor(project), adaptee, scope, clazz);
     }

     private Object getNaturalJavaElementAdapter(Object adaptee, MetricsScope scope, Class clazz) throws CoreException {
         if (adaptee instanceof JavaElementAdapter) {
             return getNaturalJavaElementAdapter(((JavaElementAdapter)adaptee).getAdaptee(), scope, clazz);
         } else if (adaptee instanceof IJavaElement) {
             return getNaturalAdapterOnProject(
                 ((IJavaElement)adaptee).getJavaProject().getProject(), adaptee, scope, clazz);
         } else if (adaptee instanceof IAdaptable) {
             return getNaturalJavaElementAdapter(
                 ((IAdaptable)adaptee).getAdapter(IJavaElement.class), scope, clazz);
         } else {
             return null;
         }
     }

     private Object getMetrics(CloverProject cloverProject, Object adaptee, MetricsScope scope, Class clazz) {
         HasMetrics metricsProvider = null;
         if (cloverProject != null) {
             metricsProvider = cloverProject.getModel().metricsProviderFor(adaptee, scope);
         }

         if (metricsProvider != null &&  BlockMetrics.class.isAssignableFrom(clazz)) {
             return metricsProvider.getMetrics();
         } else {
             return metricsProvider;
         }
     }
 }
