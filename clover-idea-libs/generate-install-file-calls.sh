#!/usr/bin/env bash
# Generates a full standalone Maven pom (install-helper.pom) with maven-install-plugin
# executions to install all IDEA JARs from lib/ and selected plugins/ to local Maven repo.
#
# Usage  : $0 <idea-folder> <idea-version>
# Example: $0 /path/to/target/extract/idea-IU-261.25134.95 261.25134.95
# Output : stdout — redirect to target/generated/install-helper.pom

help() {
  echo ""
  echo "Generates install-helper.pom — a standalone Maven pom with maven-install-plugin"
  echo "executions for all IDEA JARs found in <idea-folder>."
  echo ""
  echo "Usage  : $0 <idea-folder> <idea-version>"
  echo "Example: $0 /path/to/idea-IU-261.25134.95 261.25134.95"
  echo ""
}

# $1 idea folder (absolute path)
# $2 idea version
scanLibraries() {
  groupId="org.openclover.idea.libs"
  libDir=$1/lib
  version=$2

  for libFile in $libDir/*.jar; do
    artifactId=$(echo "$libFile" | sed 's/\.jar//' | sed 's/.*\///')
    echoExecution "$groupId" "$artifactId" "$version" "$libFile"
  done

  # scan lib/modules/*.jar (present in IDEA 2024+)
  if [ -d "$libDir/modules" ]; then
    for libFile in $libDir/modules/*.jar; do
      artifactId=$(echo "$libFile" | sed 's/\.jar//' | sed 's/.*\///')
      echoExecution "$groupId" "$artifactId" "$version" "$libFile"
    done
  fi
}

# $1 idea folder (absolute path)
# $2 idea version
scanPlugins() {
  groupId="org.openclover.idea.plugins"
  pluginsDir=$1/plugins
  libDir=$1/lib
  version=$2
  pluginIncludes="properties devkit java"
  for pluginDir in $(ls "$pluginsDir"); do
    if [ "$(echo "$pluginIncludes" | grep -w "$pluginDir" | wc -l)" -ne 0 ]; then
      for pluginFile in $(ls "$pluginsDir/$pluginDir/lib/$pluginDir"*.jar "$pluginsDir/$pluginDir/lib/jps-"*.jar 2>/dev/null | sort -u); do
        pluginFileName=$(echo "$pluginFile" | sed 's/\.jar//' | sed 's/.*\///')
        if [ -f "$libDir/$pluginFileName.jar" ]; then
          continue
        fi
        echoExecution "$groupId" "$pluginFileName" "$version" "$pluginFile"
      done
      # scan lib/modules/*.jar (plugin sub-modules, present in IDEA 2024+)
      if [ -d "$pluginsDir/$pluginDir/lib/modules" ]; then
        for pluginFile in "$pluginsDir/$pluginDir/lib/modules/"*.jar; do
          pluginFileName=$(echo "$pluginFile" | sed 's/\.jar//' | sed 's/.*\///')
          echoExecution "$groupId" "$pluginFileName" "$version" "$pluginFile"
        done
      fi
    fi
  done
}

echoExecution() {
  groupId=$1
  artifactId=$2
  version=$3
  file=$4

  echo "            <execution>"
  echo "                <id>install-idea-$artifactId</id><phase>install</phase><goals><goal>install-file</goal></goals>"
  echo "                <configuration>"
  echo "                    <file>$file</file><packaging>jar</packaging><generatePom>true</generatePom>"
  echo "                    <groupId>$groupId</groupId><artifactId>$artifactId</artifactId><version>$version</version>"
  echo "                </configuration>"
  echo "            </execution>"
}

if [ "$#" -ne 2 ]; then
  help
  exit 1
fi

IDEA_DIR=$1
IDEA_VERSION=$2

cat <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.openclover.idea.libs</groupId>
    <artifactId>install-helper</artifactId>
    <version>$IDEA_VERSION</version>
    <packaging>pom</packaging>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-install-plugin</artifactId>
                <version>3.1.1</version>
                <executions>
EOF

scanLibraries "$IDEA_DIR" "$IDEA_VERSION"
scanPlugins "$IDEA_DIR" "$IDEA_VERSION"

cat <<'EOF'
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF
