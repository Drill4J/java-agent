#!/bin/bash

# Artifactory location
server=https://drill4j.jfrog.io/artifactory
repo=drill

# Maven artifact location
name=drill-agent-linuxX64
artifact=com/epam/drill
path=$server/$repo/$artifact
echo $path
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
echo $version
build=$(curl -s $path/$version/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
echo $build

# Download
# echo $url
# wget -q -N $url
