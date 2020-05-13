#!/usr/bin/env bash

# Extract properties from manifest.json to convert them to *.properties files

# REQUIRES: jq - https://stedolan.github.io/jq/

MANIFEST_FILE=${1:-manifest.json}

# Manifest Spec
# https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/SHIP/en-US/3d562b85b37a460a92d32ec991459133.html

PERSONAS=(
  'null'
  '"development"'
  '"production"'
  '"staging"'
)
ASPECTS=( 
  '"accstorefront"'
  '"admin"'
  '"api"'
  '"backgroundProcessing"' 
  '"backoffice"'
)

# top-level properties
echo ''
for i in "${PERSONAS[@]}"
do
    echo "-------------Persona: $i-------------"
    echo ''
    jq --raw-output \
      ".properties[] | select(.persona == $i) | .key + \"=\" + .value" \
      "$MANIFEST_FILE" \
    | sort | uniq
    echo ''
done

# aspect-specific properties
for i in "${ASPECTS[@]}"
do
    for j in "${PERSONAS[@]}"
    do
        echo "-------------Aspect: $i | Persona: $j-------------"
        echo ''
        jq --raw-output \
          ".aspects[] | select(.name == $i) |
          .properties[]? | select(.persona == $j) | .key + \"=\" + .value" \
          "$MANIFEST_FILE" \
        | sort | uniq
        echo ''
    done
done