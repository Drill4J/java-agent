#!/bin/bash

cp -R /data-download/* /data
nginx -g 'daemon off;'
