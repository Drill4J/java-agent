#!/bin/bash

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
# https://drill4j.jfrog.io/artifactory/drill/com/epam/drill
# https://drill4j.jfrog.io/artifactory/drill/com/epam/drill/maven-metadata.xml
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
echo "version:"
echo $version
echo "path/maven-metadata.xml:"
echo $path//maven-metadata.xml
build=$(curl -s $path//maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
echo "build:"
echo $build
jar=$name-$build.jar
echo "jar:"
echo $jar
url=$path/$version/$jar
echo "$url:"
echo $url

# Download
# echo $url
# wget -q -N $url
