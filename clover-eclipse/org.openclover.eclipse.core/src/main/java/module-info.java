module org.openclover.eclipse.core {
    requires asm;
    requires org.eclipse.core.resources;
    requires org.eclipse.equinox.common;
    requires org.eclipse.jdt.core;

    requires org.openclover.runtime;
    requires org.openclover.core;

    exports org.openclover.eclipse.core.exclusion;
    exports org.openclover.eclipse.core.launching.actions;
    exports org.openclover.eclipse.core.launching;
    exports org.openclover.eclipse.core.projects.builder;
    exports org.openclover.eclipse.core.projects.model;
    exports org.openclover.eclipse.core.projects.settings.source.test;
    exports org.openclover.eclipse.core.projects.settings.source;
    exports org.openclover.eclipse.core.projects.settings;
    exports org.openclover.eclipse.core.projects;
    exports org.openclover.eclipse.core.reports.model;
    exports org.openclover.eclipse.core.reports.reporters;
    exports org.openclover.eclipse.core.reports;
    exports org.openclover.eclipse.core.settings.upgrade;
    exports org.openclover.eclipse.core.settings;
    exports org.openclover.eclipse.core.ui.editors.cloud;
    exports org.openclover.eclipse.core.ui.editors.java.actions;
    exports org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space;
    exports org.openclover.eclipse.core.ui.editors.java.annotations.strategies;
    exports org.openclover.eclipse.core.ui.editors.java;
    exports org.openclover.eclipse.core.ui.editors.treemap;
    exports org.openclover.eclipse.core.ui.editors;
    exports org.openclover.eclipse.core.ui.projects.widgets;
    exports org.openclover.eclipse.core.ui.projects;
    exports org.openclover.eclipse.core.ui.widgets;
    exports org.openclover.eclipse.core.ui.workingset;
    exports org.openclover.eclipse.core.ui;
    exports org.openclover.eclipse.core.upgrade.hooks;
    exports org.openclover.eclipse.core.views.actions;
    exports org.openclover.eclipse.core.views.coverageexplorer.actions;
    exports org.openclover.eclipse.core.views.coverageexplorer.nodes;
    exports org.openclover.eclipse.core.views.coverageexplorer.widgets;
    exports org.openclover.eclipse.core.views.coverageexplorer;
    exports org.openclover.eclipse.core.views.dashboard;
    exports org.openclover.eclipse.core.views.nodes;
    exports org.openclover.eclipse.core.views.testcontributions;
    exports org.openclover.eclipse.core.views.testrunexplorer.actions;
    exports org.openclover.eclipse.core.views.testrunexplorer.nodes;
    exports org.openclover.eclipse.core.views.testrunexplorer.widgets;
    exports org.openclover.eclipse.core.views.testrunexplorer;
    exports org.openclover.eclipse.core.views.widgets.columns;
    exports org.openclover.eclipse.core.views.widgets.context;
    exports org.openclover.eclipse.core.views.widgets;
    exports org.openclover.eclipse.core.views;
    exports org.openclover.eclipse.core;
    exports org.openclover.eclipse.core.compiler;
}