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

### Download KTremap fork and install it

```
git clone https://bitbucket.org/atlassian/ktreemap
cd ktreemap
git checkout ktreemap-1.1.0-atlassian-01           
# an old maven-antrun-plugin does not recognize <target> tag
sed -i -e 's@<artifactId>maven-antrun-plugin</artifactId>@<artifactId>maven-antrun-plugin</artifactId><version>3.1.0</version>@' pom.xml
# maven dependency plugin fails because of missing eclipse artifact so copy it manually
mkdir -p target/eclipse
cp ../clover-eclipse-libs/target/extract/*.jar target/eclipse           
mvn install -Dmdep.skip=true  
cd ..
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
