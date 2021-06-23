#!/bin/bash

if [ ! -f /data/agent/libdrill_agent.so ]; then
    echo "File /data/agent/libdrill_agent.so not found!"
    cp -R /data-download/* /data
fi
