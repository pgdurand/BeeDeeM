#PDB_proteins
#Fri Sep 15 13:47:28 CEST 2017

db.name=PDB_proteins
db.desc=PDB Protein databank; illustrate use of external script call
db.type=p
db.ldir=${mirrordir}|p|PDB_proteins

db.files.include=pdbaa_v4.tar.gz
db.files.exclude=

tasks.global.pre=script(name=WaitALittle;path=wait_a_little.sh)
tasks.global.post=makealias,delgz,deltar,script(name=HelloWorld;path=hello_world.sh)

tasks.unit.post=gunzip,untar,script(name=HelloWorld;path=hello_world.sh)

ftp.uname=anonymous
ftp.pswd=user@institute.org
ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.rdir=/blast/db/v4
ftp.rdir.exclude=

depends=

history=0

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M

