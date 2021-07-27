#!/usr/bin/env bash

# Extract extensions from a manifest.json to use them in localextensions.xml

# REQUIRES: jq - https://stedolan.github.io/jq/

MANIFEST_FILE=${1:-manifest.json}

#get extension list
echo ''
echo '---------------localextensions.xml-------------------'
echo ''
extensions=$(jq ".extensions[]" "$MANIFEST_FILE")
for extension in $extensions
do
    echo "<extension name=$extension />"
done
 
echo ''
echo "--------------------------${ENVIRONMENT}.properties--------------------------"
echo ''
