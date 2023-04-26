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
  echo "UPDATING VERSION NUMBER TO $version"
  mvn org.codehaus.mojo:versions-maven-plugin:2.15.0:set -DnewVersion=$version -DgenerateBackupPoms=false || exit 1
  git add -u || exit 1
}

tagReleaseAndPush() {
  version="release-$1"
  echo "TAGGING VERSION AS $version"
  git tag "$version" || exit 1
  git push --tags || exit 1
}

commitNoPush() {
  message="$1"
  echo "COMMITTING CHANGES $message"
  git commit -m "$message" || exit 1
}

commitAndPush() {
  message="$1"
  echo "COMMITTING AND PUSHING CHANGES $message"
  git commit -m "$message" || exit 1
  git push || exit 1
}

checkoutAndDeploy() {
  version="release-$1"
  echo "CHECKING OUT $version"
  git checkout "$version" || exit 1
  echo "BUILDING AND DEPLOYING $version"
  mvn clean deploy -DskipTests=true -Dmaven.deploy.skip=true  || exit 1
}

checkoutMaster() {
  echo "CHECKING OUT master"
  git checkout master || exit 1
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
commitNoPush "Prepare release $releaseNumber"
tagReleaseAndPush "$releaseNumber"

updateVersionNumber "$nextReleaseNumber"
commitAndPush "Prepare for next development iteration $nextReleaseNumber"

checkoutAndDeploy "$releaseNumber"
checkoutMaster

echo "DONE"
