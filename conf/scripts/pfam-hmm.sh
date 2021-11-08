#!/usr/bin/env bash

# As a reminder, a BeeDeeM Post processing script is called
# with with these arguments: -w <path> -d <path> -f <path> -n <name> -t <type>
#
#  -w <path>: <path> is the working directory path.
#             provided for both unit and global tasks.
#  -d <path>: <path> is the bank installation path.
#             provided for both unit and global tasks.
#  -f <path>: <path> is the path to file under unit task processing
#             only provided with unit task.
#  -n <name>: <name> is the bank name.
#  -t <type>: <path> is the bank type. One of p, n or d.
#             p: protein
#             n: nucleotide
#             d: dictionary or ontology

echo "Running script: $0"
echo "     arguments: $@"

DB_PATH=
DBFILE=

# So, we handle that here...
while getopts w:d:f:n:t: opt
do
    case "$opt" in
      w)  WK_DIR="$OPTARG";;
      d)  INST_DIR="$OPTARG";;
      f)  PROCESSED_FILE="$OPTARG";;
      n)  BANK_NAME="$OPTARG";;
      t)  BANK_TYPE="$OPTARG";;
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

