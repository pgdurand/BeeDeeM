#!/usr/bin/env bash
#
# A script to be used within a Docker container: it aims at starting BeeDeeM task
# given some parameters.
#
# Use: ./run_bdm.sh -c <command> <arguments>
#
#        <command>: one of install, query, annotate, info
#      <arguments>: remaining arguments passed in after <command> are passed
#                   to the appropriate BeeDeeM program: install.sh, query.sh,
#                   annotate.sh or info.sh. Please refer to these programs to 
#                   review their expected arguments.
#
# Copyright (c) 2017, Patrick G. Durand
# ========================================================================================
# Section: utility function declarations
# --------
# FUNCTION: display help message
function help(){
  printf "\n$0: a tool to invoke BeeDeeM within a Docker container.\n\n"
  printf "usage: $0 -c <command> [arguments]\n\n"
  exit 1
}

# ========================================================================================
# Section: Main

# Prepare arguments for processing
while getopts hc: opt
do
  case "$opt" in
    c)  COMMAND="$OPTARG";;
    h)  help;;
    \?) help;;
  esac
done
shift `expr $OPTIND - 1`

# remaining arguments, if any, are supposed to be the [file ...] part of the command-line
ALL_ARGS=$@

#execute command
case "$COMMAND" in
  install)
    . /opt/beedeem/install.sh $ALL_ARGS
    ;;
  query)
    . /opt/beedeem/query.sh $ALL_ARGS
    ;;
  annotate)
    . /opt/beedeem/annotate.sh $ALL_ARGS
    ;;
  info)
    . /opt/beedeem/info.sh
    ;;
esac

exit 0
