#!/bin/bash

echo "run commands.sh"

echo "list in directory /data:"
ls --color=auto /data

echo "mkdir -p /data/agent:"
mkdir -p /data/agent

echo "list in directory /data-download/agent:"
ls --color=auto /data-download/agent

cp -R /data-download/agent/* /data/agent

echo "list in directory /data/agent:"
ls --color=auto /data/agent