package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageData;
import org.openclover.core.MaskedBitSetCoverageProvider;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.MethodSignatureInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ParameterInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.eclipse.core.projects.CloverProject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class LoadedDatabaseModel extends StableDatabaseModel {
    protected CloverDatabase database;

    public LoadedDatabaseModel(CloverProject project, CloverDatabase database, CoverageModelChangeEvent changeEvent, boolean includeFailedCoverage) {
        super(project, changeEvent);

        // Apply filter excluding coverage from failed test cases
        if (!includeFailedCoverage) {
            final CoverageData data = database.getCoverageData();
            final CoverageDataProvider provider = new MaskedBitSetCoverageProvider(data.getPassOnlyAndIncidentalHits(), data, data);
            database.getFullModel().setDataProvider(provider);
            database.getAppOnlyModel().setDataProvider(provider);
            database.getTestOnlyModel().setDataProvider(provider);
        }

        this.database = database;
    }

    @Override
    public CloverDatabase getDatabase() {
        return database;
    }

    @Override
    public void refreshCoverage(CoverageModelChangeEvent changeEvent) {
        project.setModel(new RefreshingCoverageModel(this, database, changeEvent));
    }
    
    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public boolean isCoverageOutOfDate() {
        return !database.isRecordingInProgress() && database.isCoverageOutOfDate();
    }

    @Override
    public boolean isRegistryOfDate() {
        return database.isRegistryOutOfDate();
    }

    @Override
    public ProjectInfo getFullProjectInfo() {
        return database.getFullModel();
    }

    @Override
    public ProjectInfo getTestOnlyProjectInfo() {
        return database.getTestOnlyModel();
    }

    @Override
    public ProjectInfo getAppOnlyProjectInfo() {
        return database.getAppOnlyModel();
    }

    @Override
    public HasMetrics getPackageInfoOrFragment(IPackageFragment pack, MetricsScope scope) {
        HasMetrics result =
            scope.getProjectInfoFor(project).getNamedPackage(pack.getElementName());
        return
            result == null
            ? scope.getProjectInfoFor(project).findPackageFragment(pack.getElementName())
            : result;
    }

    @Override
    public FileInfo getSourceFileInfo(ICompilationUnit cu, MetricsScope scope) {
        PackageInfo packageInfo = scope.getProjectInfoFor(project).getNamedPackage(cu.getParent().getElementName());
        return packageInfo == null ? null : packageInfo.getFileInPackage(cu.getElementName());
    }

    @Override
    public ClassInfo getTypeInfo(IType type, MetricsScope scope) {
        //Here we make sure we get back Inner.Classes rather than Inner$Classes which confuses Clover
        return scope.getProjectInfoFor(project).findClass(type.getFullyQualifiedName('.'));
    }

    @Override
    public MethodInfo getMethodInfo(IMethod method, MetricsScope scope) {
        ClassInfo classInfo = scope.getProjectInfoFor(project).findClass(method.getDeclaringType().getFullyQualifiedName('.'));
        if (classInfo instanceof FullClassInfo) {
            for (MethodInfo methodInfo : classInfo.getMethods()) {
                MethodSignatureInfo sig = methodInfo.getSignature();
                if (sig.getName().equals(method.getElementName())) {
                    String[] paramTypes1 = toEclipseSignatures(sig.getParameters());
                    String[] paramTypes2 = method.getParameterTypes();
                    if (Arrays.equals(paramTypes2, paramTypes1)) {
                        return methodInfo;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public TestCaseInfo getTestCaseInfo(IMethod method, MetricsScope scope) {
        ClassInfo classInfo = scope.getProjectInfoFor(project).findClass(method.getDeclaringType().getFullyQualifiedName('.'));
        if (classInfo instanceof FullClassInfo) {
            Collection<TestCaseInfo> testCases = classInfo.getTestCases();
            for (TestCaseInfo testCase : testCases) {
                MethodInfo methodInfo = testCase.getSourceMethod();
                if (methodInfo != null) {
                    MethodSignatureInfo sig = methodInfo.getSignature();
                    if (sig.getName().equals(method.getElementName())) {
                        String[] paramTypes1 = toEclipseSignatures(sig.getParameters());
                        String[] paramTypes2 = method.getParameterTypes();
                        if (Arrays.equals(paramTypes2, paramTypes1)) {
                            return testCase;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public TestCaseInfo[] getTestCaseInfos(IMethod method, MetricsScope scope) {
        ClassInfo classInfo = scope.getProjectInfoFor(project).findClass(method.getDeclaringType().getFullyQualifiedName('.'));
        if (classInfo instanceof FullClassInfo) {
            Collection<TestCaseInfo> testCases = classInfo.getTestCases();
            for (Iterator<TestCaseInfo> iter = testCases.iterator(); iter.hasNext();) {
                TestCaseInfo testCase = iter.next();
                MethodInfo methodInfo = testCase.getSourceMethod();
                if (methodInfo != null) {
                    MethodSignatureInfo sig = methodInfo.getSignature();
                    if (!sig.getName().equals(method.getElementName())
                        || !Arrays.equals(method.getParameterTypes(), toEclipseSignatures(sig.getParameters()))) {
                        iter.remove();
                    }
                }
            }
            return testCases.toArray(new TestCaseInfo[0]);
        }
        return new TestCaseInfo[] {};
    }

    private String[] toEclipseSignatures(ParameterInfo[] signatures) {
        String[] eclipseSignatures = new String[signatures.length];
        for (int i = 0; i < eclipseSignatures.length; i++) {
            eclipseSignatures[i] = toEclipseSignature(signatures[i].getType());
        }
        return eclipseSignatures;
    }

    private String toEclipseSignature(String sig) {
        return Signature.createTypeSignature(sig, false);
    }

    @Override
    public HasMetrics metricsProviderFor(Object object, MetricsScope scope) {
        if (object instanceof IProject) {
            return scope.getProjectInfoFor(project);
        } if (object instanceof IPackageFragment) {
            return getPackageInfoOrFragment((IPackageFragment) object, scope);
        } else if (object instanceof ICompilationUnit) {
            return getSourceFileInfo((ICompilationUnit) object, scope);
        } else if (object instanceof IType) {
            return getTypeInfo((IType)object, scope);
        } else if (object instanceof IMethod) {
            return getMethodInfo((IMethod)object, scope);
        } else {
            return null;
        }
    }
}
