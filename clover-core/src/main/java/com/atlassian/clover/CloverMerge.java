package com.atlassian.clover;

import com.atlassian.clover.cfg.Interval;

import java.util.List;
import java.util.Map;

import static clover.com.google.common.collect.Lists.newLinkedList;
import static clover.com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class CloverMerge {

    private static String initString;
    private static Map<String, Interval> mergingDbs;
    private static boolean update = false;
    private static Interval updateSpan = Interval.DEFAULT_SPAN;

    public static void main(final String[] args) {
        System.exit(mainImpl(args));
    }
    /**
     * sames as "main", but returns 0 on success
     */
    public static int mainImpl(final String[] args) {
        if (!processArgs(args)) {
            return 1;
        }
        final List<CloverDatabaseSpec> databases = newLinkedList();
        for (String db : mergingDbs.keySet()) {
            databases.add(new CloverDatabaseSpec(db, mergingDbs.get(db)));
        }
        try {
            CloverDatabase.merge(databases, initString, update, updateSpan, new ProgressListener() {

                @Override
                public void handleProgress(String desc, float pc) {
                    System.out.println(desc);
                }
            });
        }
        catch (Exception e) {
            Logger.getInstance().error("Error writing new clover db '" + initString, e);
            return 1;
        }
        return 0;
    }


    private static void usage(String msg) {
        System.err.println();
        if (msg != null) {
            System.err.println("  *** ERROR: " + msg);
        }
        System.err.println();
        System.err.println("  USAGE: " + CloverMerge.class.getName() + " [OPTIONS] PARAMS [DBFILES...]");
        System.err.println();
        System.err.println("  PARAMS:");
        System.err.println("    -i, --initstring <file>\t Clover initstring. This is the path where the new merged");
        System.err.println("\t\t\t\t database will be written.");
        System.err.println();
        System.err.println();
        System.err.println("  OPTIONS:");
        System.err.println("    -u, --update [interval]\t if specified, any existing database specified by -i will be included in the");
        System.err.println("\t\t\t\t merge. If interval is specified, it is used as the span when reading the existing database.");
        System.err.println("    -s, --span <interval>\t Specifies the span to use when reading subsequent databases to be ");
        System.err.println("\t\t\t\t merged. This option can be specified more than once and applies to all databases ");
        System.err.println("\t\t\t\t specified after the option, or until another span in specified.");
        System.err.println("    -v, --verbose\t\t Enable verbose logging.");
        System.err.println("    -d, --debug\t\t Enable debug logging.");
    }

    private static boolean processArgs(String[] args) {

        try {
            mergingDbs = newHashMap();
            int i = 0;

            Interval interval = Interval.ZERO_SECONDS;

            while (i < args.length) {
                if (args[i].equals("-i") || args[i].equals("--initstring")) {
                    i++;
                    initString = args[i];
                }
                else if (args[i].equals("-u") || args[i].equals("--update")) {
                    update = true;
                    if (i < args.length - 1) {
                        try {
                            i++;
                            updateSpan = new Interval(args[i]);
                        }
                        catch (NumberFormatException e) {
                            // ignore this. if a user has specified an update span, but incorrectly, it will be
                            // reported as an error later anyway.
                            i--;
                        }
                    }
                }
                else if (args[i].equals("-s") || args[i].equals("--span")) {
                    i++;
                    try {
                        interval = new Interval(args[i]);
                    }
                    catch (NumberFormatException e) {
                        usage("Bad interval format '"+args[i]+"'");
                        return false;
                    }
                }
                else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                    Logger.setVerbose(true);
                }
                else if (args[i].equals("-d") || args[i].equals("--debug")) {
                    Logger.setDebug(true);
                }
                else {
                    mergingDbs.put(args[i], interval);
                }
                i++;
            }

            if (initString == null) {
                usage("Need to specify an initstring for the merged database.");
                return false;
            }
            else if (mergingDbs.size() == 0 && !update) {
                usage("No databases to merge.");
                return false;
            }
            else {
                return true;
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            usage("Missing a parameter.");
        }
        return false;
    }
}
