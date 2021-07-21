#!/bin/bash

echo "run commands.sh"
echo ""

echo "list in directory /data/agent:"
ls --color=auto /data/agent || true
echo ""

echo "mkdir -p /data/agent"
mkdir -p /data/agent
echo ""

echo "list in directory /data-download/agent:"
ls --color=auto /data-download/agent
echo ""

echo "cp -R /data-download/agent/* /data/agent:"
cp -R /data-download/agent/* /data/agent
echo ""

echo "list in directory /data/agent:"
ls --color=auto /data/agent
echo ""