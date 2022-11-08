#!/usr/bin/env bash

# just dump little messages...
echo "Hello world"
if [ "$#" -ne 1 ]; then
  echo "Passed in arguments: $@"
fi

printf "Log an ERROR in log file\n" >&2
