# Illustrates the use of various scripts (external tasks)
# DO NOT use it for production!

# Descriptor documentation, see:
#  https://pgdurand.gitbook.io/beedeem/getting-started/descriptors-format

db.name=PDB_proteins_task
db.desc=PDB Protein databank; illustrate use of external script call
db.type=p
db.ldir=${mirrordir}|p|PDB_proteins_task

db.files.include=pdbaa.tar.gz
db.files.exclude=

# This a pre-processing script; called once at the very beginning of bank precessing
#   Illustrate a script call with additional arguments; setting an argument with '=NA" defines a no-arg parameter
# Pre-processing script is optional.
tasks.global.pre=script(name=WaitALittle;path=wait_a_little),script(name=HelloWorld;path=hello_world;-parse_seqid=NA;-k=19;-w=15;--verbose-mode=debug)

tasks.global.post=script(name=WaitALittle;path=wait_a_little),makealias,delgz,deltar,script(name=HelloWorld;path=hello_world)

tasks.unit.post=script(name=WaitALittle;path=wait_a_little),gunzip,untar,script(name=HelloWorld;path=hello_world)

ftp.uname=anonymous
ftp.pswd=user@institute.org
ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.rdir=/blast/db
ftp.rdir.exclude=

depends=

history=0

aspera.use=false
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M

