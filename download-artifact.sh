#!/bin/bash

if [ "$AGENT_VERSION" = "latest" ]
then
      echo "Variable $AGENT_VERSION is latest"
      echo "Download agent $latest version"
      # Artifactory location
      server=https://drill4j.jfrog.io/artifactory
      repo=drill
      # Maven artifact location
      name=drill-agent-linuxX64
      artifact=com/epam/drill/$name
      path=$server/$repo/$artifact
      echo "path:"
      echo $path
      path=$server/$repo/$artifact
      version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
      echo "version:"
      echo $version
      echo "github_url"
      github_url="https://github.com/Drill4J/java-agent/releases/download/v$version/agent-linuxX64-$version.zip"
      echo $github_url

      # Download
      wget -q -N $github_url
      unzip -o agent-linuxX64-$version.zip
      cp -R linuxX64-$version/* /data/agent/

else
      echo "Variable $AGENT_VERSION is NOT latest"
      echo "Download agent $AGENT_VERSION"
      AGENT_VERSION = $(echo "$AGENT_VERSION" | sed 's/v//')
      github_url="https://github.com/Drill4J/java-agent/releases/download/v$AGENT_VERSION/agent-linuxX64-$AGENT_VERSION.zip"
      echo $github_url

      # Download
      wget -q -N $github_url
      unzip -o agent-linuxX64-$AGENT_VERSION.zip
      cp -R linuxX64-$AGENT_VERSION/* /data/agent/
fi


