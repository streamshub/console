#!/usr/bin/env bash

YQ="$(which yq 2>/dev/null)" || :

if [ "${YQ}" == "" ] ; then
    echo -e "${ERROR} 'yq' command line utility found on the PATH"
    exit 1
fi

SKOPEO="$(which skopeo 2>/dev/null)" || :

if [ "${SKOPEO}" == "" ] ; then
    echo "skopeo is not installed, please install"
    exit 1
fi