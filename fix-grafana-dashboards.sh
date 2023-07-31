#!/usr/bin/env bash

# generate patches (replace HEAD with the commit you want to diff against)
#find docker/grafana/dashboards -type f -iname '*.json' -exec bash -c 'git diff --patch HEAD~1 ${1} > ${1}.patch' -- {} \;

# apply patches
find docker/grafana/dashboards -type f -iname '*.json.patch' -exec bash -c 'git apply ${1}' -- {} \;
