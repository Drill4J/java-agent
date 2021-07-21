#!/bin/bash

echo "run commands.sh"

echo "ls --color=auto /data:"
ls --color=auto /data

echo "mkdir -p /data/agent:"
mkdir -p /data/agent

echo "ls --color=auto /data-download/agent:"
ls --color=auto /data-download/agent

cp -R /data-download/agent/* /data/agent

echo "ls --color=auto /data/agent:"
ls --color=auto /data/agent