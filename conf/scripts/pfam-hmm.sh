#!/usr/bin/env bash

# As a reminder, a BeeDeeM Post processing script is called
# with up to two arguments:  -d <path> -f <path>
#
#  -d <path>: <path> is the bank installation path.
#             provided for both unit and global tasks.
#  -f <path>: <path> is the path to file under unit task processing
#             only provided with unit task.

echo "Running script: $0"
echo "     arguments: $@"

DB_PATH=
DBFILE=

# We handle BeeDeeM provided arguments here...
while getopts d:f: opt
do
    case "$opt" in
      d)  DB_PATH="$OPTARG";;
      f)  DB_FILE="$OPTARG";;
    esac
done
shift `expr $OPTIND - 1`

# To handle Pfam hmmer file, we must have DB_PATH not empty
if [ -z "${DB_PATH}" ]; then
  printf "/!\ Missing mandatory argument: -d <path-to-db-install-dir>\n" >&2
  exit 1
fi

echo "Entering PFAM install directory: ${DB_PATH}"
cd ${DB_PATH}
echo "Activating HMMER 3.3 Conda env"
. /appli/bioinfo/hmmer/3.3/env.sh
echo "Running hmmpress -f Pfam-A.hmm"
hmmpress -f Pfam-A.hmm
RET_CODE=$?
echo "Deactivating Conda env"
. /appli/bioinfo/hmmer/3.3/delenv.sh

exit $RET_CODE
