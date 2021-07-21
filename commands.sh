#!/bin/bash

echo "run commands.sh"

echo "list in directory /data:"
ls --color=auto /data
echo "\n"

echo "mkdir -p /data/agent:"
mkdir -p /data/agent
echo "\n"

echo "list in directory /data-download/agent:"
ls --color=auto /data-download/agent
echo "\n"

echo "cp -R /data-download/agent/* /data/agent:"
cp -R /data-download/agent/* /data/agent
echo "\n"

echo "list in directory /data/agent:"
ls --color=auto /data/agent
echo "\n"