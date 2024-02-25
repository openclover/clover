[![GitHub](https://img.shields.io/badge/license-Apache%202.0-silver.svg)](https://github.com/openclover/clover/blob/master/LICENSE.txt)
[![GitHub commit activity (branch)](https://img.shields.io/github/commit-activity/y/openclover/clover/master)](https://github.com/openclover/clover/commits/master)
[![GitHub last commit (branch)](https://img.shields.io/github/last-commit/openclover/clover/master)](https://github.com/openclover/clover/commits/master)
[![Generic badge](https://img.shields.io/badge/Website-openclover.org-green.svg)](https://openclover.org/)
[![RSS](https://img.shields.io/badge/rss-F88900?logo=rss&logoColor=white)](https://openclover.org/blog-rss.xml)

[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover/A-build-and-test.yml?label=JDK8)](https://github.com/openclover/clover/actions/workflows/A-build-and-test.yml)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover/A-build-and-test-jdk11.yml?label=JDK11)](https://github.com/openclover/clover/actions/workflows/A-build-and-test-jdk11.yml)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover/A-build-and-test-jdk17.yml?label=JDK17)](https://github.com/openclover/clover/actions/workflows/A-build-and-test-jdk17.yml)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/openclover/clover/A-build-and-test-jdk21.yml?label=JDK21)](https://github.com/openclover/clover/actions/workflows/A-build-and-test-jdk21.yml)


[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/11)](https://github.com/openclover/clover/milestone/11)
[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/14)](https://github.com/openclover/clover/milestone/14)
[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/15)](https://github.com/openclover/clover/milestone/15)
[![GitHub milestone details](https://img.shields.io/github/milestones/progress-percent/openclover/clover/4)](https://github.com/openclover/clover/milestone/4)

# About #

This repository contains source code of OpenClover Core as well as its integrations: Clover-for-Ant, Clover-for-Eclipse
and Clover-for-IDEA plugins. Sources are licensed under Apache 2.0 license.

# Documentation #

User documentation:

* https://openclover.org/documentation
* https://confluence.atlassian.com/display/CLOVER/Clover+Documentation+Home

Developer guides:

* https://openclover.org/documentation
* https://confluence.atlassian.com/display/CLOVER/Clover+Development+Hub

Support Knowledge Base:

* https://openclover.org/documentation
* https://confluence.atlassian.com/display/CLOVERKB/Clover+Knowledge+Base+Home

Q&A forums:

* Stackoverflow: https://stackoverflow.com/tags/clover
* Atlassian Community: https://community.atlassian.com/t5/Clover/ct-p/clover

Bug and feature tracker:

* https://github.com/openclover/clover/issues
* https://jira.atlassian.com/browse/CLOV

Download page:

* https://openclover.org/downloads

Source code:

* https://github.com/openclover/clover

See also:

* https://github.com/openclover/clover-maven-plugin
* https://github.com/openclover/gradle-clover-plugin
* https://github.com/openclover/grails-clover-plugin
* https://github.com/openclover/clover-examples
* https://github.com/openclover/clover-aspectj-compiler
* https://github.com/jenkinsci/clover-plugin
* https://github.com/hudson3-plugins/clover-plugin

# Quick setup for developing OpenClover

### Install JDK 1.8, Ant 1.10+, Maven 3.8+, Git

### Prepare repacked third party libraries

```
mvn install -f clover-core-libs/jarjar/pom.xml
mvn install -Pworkspace-setup -f clover-core-libs/pom.xml
mvn install -Pworkspace-setup -f clover-eclipse-libs/pom.xml
mvn install -Pworkspace-setup -f clover-jtreemap/pom.xml
mvn install -Pworkspace-setup -f clover-idea-libs/pom.xml
```

### Download KTremap and install it

Add https://packages.atlassian.com/mvn/maven-atlassian-external to your list of Maven repositories in settings.xml

OR

Download the following files and install locally:

```
PACKAGES_ATLASSIAN_COM=https://packages.atlassian.com/mvn/maven-atlassian-external/
KTREEMAP_PATH=net/sf/jtreemap/ktreemap/1.1.0-atlassian-01

wget $PACKAGES_ATLASSIAN_COM/$KTREEMAP_PATH/ktreemap-1.1.0-atlassian-01.jar
wget $PACKAGES_ATLASSIAN_COM/$KTREEMAP_PATH/ktreemap-1.1.0-atlassian-01.pom
mvn install:install-file -Dfile=ktreemap-1.1.0-atlassian-01.jar -DpomFile=ktreemap-1.1.0-atlassian-01.pom
```

Now you can work with the code using Maven. You can also open it in IntelliJ IDEA,
by importing the root pom.xml.

### Example commands

```
# Compile everything and run all tests
mvn test

# Install all modules locally, without testing 
mvn install -DskipTests=true

# Run tests for three main modules
mvn test -pl clover-ant,clover-core,clover-groovy
```

---

Copyright @ 2002 - 2017 Atlassian Pty Ltd

Copyright @ 2017 - 2023 modifications by OpenClover.org

