package com.atlassian.clover.samples;

import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;

import java.io.PrintStream;

public class SimpleRegistryDumper {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage:");
            System.err.println("java " + SimpleRegistryDumper.class.getName() + " database");
        } else {
            // read clover database together with coverage recording files, use time span=0 (latest build)
            CloverDatabase db = CloverDatabase.loadWithCoverage(args[0], new CoverageDataSpec());
            ProjectInfo projectInfo = db.getRegistry().getProject();
            // print some project details
            printProject(projectInfo, System.out);
        }
    }

    private static void printProject(ProjectInfo db, PrintStream out) {
       for (PackageInfo packageInfo : db.getAllPackages()) {
           out.println("package: " + packageInfo.getName());
           for (FileInfo fileInfo : packageInfo.getFiles()) {
               out.println("\tfile: " + fileInfo.getName());
               for (ClassInfo classInfo : fileInfo.getClasses()) {
                   out.println("\t\tclass: " + classInfo.getName());
                   for (MethodInfo methodInfo : classInfo.getMethods()) {
                       out.println("\t\t\tmethod: " + methodInfo.getName());
                   }
               }
           }
       }
    }
}
