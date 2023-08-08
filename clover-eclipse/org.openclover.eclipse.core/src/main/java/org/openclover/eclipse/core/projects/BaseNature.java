package org.openclover.eclipse.core.projects;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IClasspathEntry;
import org.openclover.eclipse.core.CloverPlugin;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import static org.openclover.util.Lists.newArrayList;

public abstract class BaseNature implements IProjectNature {
    /** Eclipse project attached to this */
    protected IProject project;

    @Override
    public IProject getProject() {
        return project;
    }

    public IJavaProject getJavaProject() throws CoreException {
        return getJavaProject(project);
    }

    protected IJavaProject getJavaProject(IProject project) throws CoreException {
        return (IJavaProject)project.getNature(JavaCore.NATURE_ID);
    }

    @Override
    public void setProject(IProject project) {
        this.project = project;
    }

    protected static List<ICommand> ensureBuilderAddedAfter(IProjectDescription description, List<ICommand> commands, String primaryId, String subsequentId, String absentId) throws CoreException {
        return ensureBuilderAdded(description, commands, false, primaryId, subsequentId, absentId);
    }

    protected static List<ICommand> ensureBuilderAddedBefore(IProjectDescription description, List<ICommand> commands, String primaryId, String subsequentId, String absentId) throws CoreException {
        return ensureBuilderAdded(description, commands, true, primaryId, subsequentId, absentId);
    }

    private static List<ICommand> ensureBuilderAdded(IProjectDescription description, List<ICommand> commands, boolean before, String primaryId, String subsequentId, String absentId) throws CoreException {
        CloverPlugin.logVerbose("adding builder " + subsequentId);

        boolean added = false;

        List<ICommand> newCommands = new ArrayList<>(commands.size() + 1);
        newCommands.addAll(commands);

        //Remove any current references
        for (int i = 0; i < newCommands.size(); i++) {
            if (newCommands.get(i).getBuilderName().equals(subsequentId)) {
                newCommands.remove(i);
                //Don't break in case there are multiple references (possible?)
            }
        }

        for (int i = 0; i < newCommands.size(); i++) {
            if (newCommands.get(i).getBuilderName().equals(primaryId)) {
                newCommands.add(before ? i : i + 1, newCommand(description, subsequentId));
                added = true;
                break;
            }
        }

        //Add at end
        if (!added) {
            boolean absentBuilderIdPresent = false;
            for (int i = 0; i < commands.size(); i++) {
                if (newCommands.get(i).getBuilderName().equals(absentId)) {
                    absentBuilderIdPresent = true;
                    break;
                }
            }
            if (!absentBuilderIdPresent) {
                newCommands.add(newCommand(description, absentId));
            }
        }

        return newCommands;
    }

    private static ICommand newCommand(IProjectDescription description, String subsequentId) {
        ICommand command = description.newCommand();
        command.setBuilderName(subsequentId);
        return command;
    }

    public static void ensureBuilderRemoved(IProject project, String builderId) throws CoreException {
        if (alreadyContainsBuilder(project, builderId)) {
            removeBuilder(project, builderId);
        }
    }

    private static boolean alreadyContainsBuilder(IProject project, String builderId) throws CoreException {
        ICommand[] commands = project.getDescription().getBuildSpec();
        for (ICommand command : commands) {
            if (command.getBuilderName().equals(builderId)) {
                return true;
            }
        }
        return false;
    }

    private static void removeBuilder(IProject project, String builderId) throws CoreException {

        IProjectDescription description = project.getDescription();
        ArrayList<ICommand> updatedCommands = newArrayList(description.getBuildSpec()); // copy

        for (Iterator<ICommand> commandIter = updatedCommands.iterator(); commandIter.hasNext();) {
            if ((commandIter.next()).getBuilderName().equals(builderId)) {
                CloverPlugin.logVerbose("removing builder " + builderId);
                commandIter.remove();
                break;
            }
        }

        description.setBuildSpec(updatedCommands.toArray(new ICommand[updatedCommands.size()]));
        project.setDescription(description, null);
    }

    protected void ensureClasspathEntryAdded(IClasspathEntry newEntry) throws CoreException {
        IJavaProject javaProject = getJavaProject();

        List<IClasspathEntry> newEntries = newArrayList(javaProject.getRawClasspath()); // copy
        for (Iterator<IClasspathEntry> iterator = newEntries.iterator(); iterator.hasNext();) {
            IClasspathEntry entry = iterator.next();
            if (similarOrSame(entry, newEntry)) {
                iterator.remove();
            }
        }
        newEntries.add(0, newEntry);

        javaProject.setRawClasspath(
            newEntries.toArray(new IClasspathEntry[newEntries.size()]),
            null);
    }

    protected boolean similarOrSame(IClasspathEntry entry1, IClasspathEntry entry2) {
        //HACK: this is not precise but I can't see many other ways to compare variable entry classpath refs
        return entry1.equals(entry2) || entry1.getPath().lastSegment().equals(entry2.getPath().lastSegment());
    }

    protected void ensureClasspathEntryAbsent(IClasspathEntry entry) throws CoreException {
        IJavaProject javaProject = getJavaProject();

        List<IClasspathEntry> oldEntries = newArrayList(javaProject.getRawClasspath()); // copy

        for (Iterator<IClasspathEntry> oldEntriesIter = oldEntries.iterator(); oldEntriesIter.hasNext();) {
            IClasspathEntry oldEntry = oldEntriesIter.next();
            if (similarOrSame(oldEntry, entry)) {
                oldEntriesIter.remove();
            }
        }

        javaProject.setRawClasspath(
            oldEntries.toArray(new IClasspathEntry[oldEntries.size()]),
            null);
    }

}
