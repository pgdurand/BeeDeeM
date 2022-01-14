#PDB_proteins
#Fri Sep 15 13:47:28 CEST 2017

# Illustrates the use of various scripts (external tasks)
# DO NOT use it for production!

db.name=PDB_proteins
db.desc=PDB Protein databank; illustrate use of external script call
db.type=p
db.ldir=${mirrordir}|p|PDB_proteins

db.files.include=pdbaa.tar.gz
db.files.exclude=

tasks.global.pre=script(name=WaitALittle;path=wait_a_little),script(name=HelloWorld;path=hello_world)
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

