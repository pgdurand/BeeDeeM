#!/usr/bin/env bash

# just dump little messages...
echo "Hello world"
if [ "$#" -ne 1 ]; then
  echo "Passed in arguments: $@"
fi

printf "ERROR \n" >&2
