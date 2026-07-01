#!/usr/bin/env bash
# Generates a full standalone Maven BOM pom (clover-idea-all.pom) listing all IDEA JARs
# from lib/ and selected plugins/ as Maven dependencies.
#
# Usage  : $0 <idea-folder> <idea-version>
# Example: $0 /path/to/target/extract/idea-IU-261.25134.95 261.25134.95
# Output : stdout — redirect to target/generated/clover-idea-all.pom

help() {
  echo ""
  echo "Generates clover-idea-all.pom — a standalone Maven BOM pom listing all IDEA JARs"
  echo "found in <idea-folder> as dependencies."
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
    echoDependency "$groupId" "$artifactId" "$version"
  done

  # scan lib/modules/*.jar (present in IDEA 2024+)
  if [ -d "$libDir/modules" ]; then
    for libFile in $libDir/modules/*.jar; do
      artifactId=$(echo "$libFile" | sed 's/\.jar//' | sed 's/.*\///')
      echoDependency "$groupId" "$artifactId" "$version"
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
        echoDependency "$groupId" "$pluginFileName" "$version"
      done
      # scan lib/modules/*.jar (plugin sub-modules, present in IDEA 2024+)
      if [ -d "$pluginsDir/$pluginDir/lib/modules" ]; then
        for pluginFile in "$pluginsDir/$pluginDir/lib/modules/"*.jar; do
          pluginFileName=$(echo "$pluginFile" | sed 's/\.jar//' | sed 's/.*\///')
          echoDependency "$groupId" "$pluginFileName" "$version"
        done
      fi
    fi
  done
}

echoDependency() {
  groupId=$1
  artifactId=$2
  version=$3

  echo "        <dependency>"
  echo "            <groupId>$groupId</groupId>"
  echo "            <artifactId>$artifactId</artifactId>"
  echo "            <version>$version</version>"
  echo "        </dependency>"
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
    <artifactId>clover-idea-all</artifactId>
    <version>$IDEA_VERSION</version>
    <packaging>pom</packaging>
    <name>IDEA Libraries as Maven Dependencies</name>
    <description>
        This pom is generated during workspace-setup and installed to the local Maven repository.
        It lists all IDEA JARs required to compile clover-idea against a given IDEA version.
        These JARs are used for classpath only and must not be bundled into clover-idea.
    </description>
    <dependencies>
EOF

scanLibraries "$IDEA_DIR" "$IDEA_VERSION"
scanPlugins "$IDEA_DIR" "$IDEA_VERSION"

cat <<'EOF'
    </dependencies>
</project>
EOF
