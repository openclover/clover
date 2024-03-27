module org.openclover.runtime {
    requires java.logging;
    requires java.rmi;
    requires clover.antlr;
    requires clover.cajo;
    requires clover.slf4j;
    requires junit;
    requires org.junit.platform.engine;
    requires org.junit.platform.launcher;

    requires org.openclover.buildutil;

    exports org.openclover.runtime.api.registry;
    exports org.openclover.runtime.api;
    exports org.openclover.runtime.recorder.junit;
    exports org.openclover.runtime.recorder.pertest;
    exports org.openclover.runtime.recorder;
    exports org.openclover.runtime.registry.format;
    exports org.openclover.runtime.registry;
    exports org.openclover.runtime.remote;
    exports org.openclover.runtime.util;
    exports org.openclover.runtime;
    exports org_openclover_runtime;
}