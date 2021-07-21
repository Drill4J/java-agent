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
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
echo "version:"
echo $version
echo "path/maven-metadata.xml:"
echo $path/maven-metadata.xml
build=$(curl -s $path/maven-metadata.xml | grep '<latest>' | head -1 | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
echo "build:"
echo $build
zip=$name-$build.zip
echo "zip:"
echo $zip
url=$path/$version/$zip
echo "url:"
echo $url

# Download
wget -q -N $url
