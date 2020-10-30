#!/usr/bin/env bash

# This is a BeeDeeM external task script template.
#
# This script illustrates the use of external tasks.
# Such a task is called from a bank descriptor; e.g. see
# for instance ../descriptors/PDB_proteins_task.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with up to two arguments: -d <path> -f <path>
#
#  -d <path>: <path> is the bank installation path.
#             provided for both unit and global tasks.
#  -f <path>: <path> is the path to file under unit task processing
#             only provided with unit task.

echo "Executing an external script"
echo "Arguments coming from BeeDeeM are:"
echo $@

