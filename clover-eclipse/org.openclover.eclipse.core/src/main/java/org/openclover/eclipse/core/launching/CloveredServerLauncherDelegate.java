package org.openclover.eclipse.core.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class CloveredServerLauncherDelegate extends CloveredLauncherDelegate {
    @Override
    protected ILaunchConfigurationWorkingCopy bindCloverRuntimeJarToLaunch(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
        workingCopy.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
            appendCloverBootclasspath(workingCopy, workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""))
        );
        return super.bindCloverRuntimeJarToLaunch(workingCopy);
    }

    private String appendCloverBootclasspath(ILaunchConfigurationWorkingCopy workingCopy, String attribute) throws CoreException {
        return
            attribute + " -Xbootclasspath/a:" + quoteIfSpaces(resolveCloverRuntimeJar(workingCopy));
    }

    private String quoteIfSpaces(String string) {
        return
            string.indexOf(' ') != -1
                ? "\"" + string + "\""
                : string;
    }
}
