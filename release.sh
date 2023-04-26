#!/usr/bin/env bash

showHelp() {
  echo "Usage:"
  echo "./release.sh <release number> <next release number>"
  echo ""
}

assertCleanWorkspace() {
  if [ `git status --short | grep -v '??' | wc -l` -ne 0 ]; then
    echo "Workspace is not clean. Cannot prepare a release."
    exit 1
  fi
}

updateVersionNumber() {
  version="$1"
  mvn org.codehaus.mojo:versions-maven-plugin:2.15.0:set -DnewVersion=$version -DremoveSnapshot=true
}

tagRelease() {
  version="release-$1"
  git tag "$version"
  git push --tags
}

commitAndPush() {
  message="$1"
  git commit -m "$message"
}

checkoutAndDeploy() {
  version="release-$1"
  git checkout "$version"
  mvn clean deploy -DskipTests=true -Dmaven.deploy.skip=true
}

############################################################

if [ "$#" -ne 2 ]; then
  showHelp
  exit 1
fi

releaseNumber="$1"
nextReleaseNumber="$2"

assertCleanWorkspace

updateVersionNumber "$releaseNumber"
commitAndPush "Prepare release $releaseNumber"
tagRelease "$releaseNumber"

updateVersionNumber "$nextReleaseNumber"
commitAndPush "Prepare for next development iteration $nextReleaseNumber"

checkoutAndDeploy "$releaseNumber"

