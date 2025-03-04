#!/bin/bash

dockerImage="7002370412/nginx"
imageTag="latest"

status=$(curl -s -f https://hub.docker.com/v2/repositories/${dockerImage}/tags/${imageTag} > /dev/null 2>&1; echo $?)

if [ "$status" -eq 0 ]; then
    echo "Image exists"
else
    echo "Image does not exist"
fi
