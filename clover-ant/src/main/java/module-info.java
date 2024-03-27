module org.openclover.ant {
    requires ant;
    requires clover.annotations;
    requires clover.commons.lang3;

    requires org.openclover.core;
    requires org.openclover.runtime;

    exports org.openclover.ant;
    exports org.openclover.ant.groovy;
    exports org.openclover.ant.taskdefs;
    exports org.openclover.ant.tasks.testng;
    exports org.openclover.ant.tasks;
    exports org.openclover.ant.types;
    exports org.openclover.ci;
}