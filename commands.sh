#!/bin/bash

echo "run commands.sh"
echo ""

echo "list in directory /data/agent:"
ls --color=auto /data/agent || true
echo ""

echo "mkdir -p /data/agent"
mkdir -p /data/agent
sleep 1
echo ""

echo "list in directory /data-download/agent:"
ls --color=auto /data-download/agent
echo ""

if [ -z "$AGENT_VERSION" ]
then
      echo "Variable $AGENT_VERSION is empty"
      echo "Used agent version from docker image"
      echo "cp -R /data-download/agent/* /data/agent:"
      cp -R /data-download/agent/* /data/agent
      echo ""
else
      echo "Variable $AGENT_VERSION is NOT empty"
      echo "Download agent version from internet"
      echo "run download-artifact.sh"
      ./download-artifact.sh
fi

echo "list in directory /data/agent:"
ls --color=auto /data/agent
echo ""
