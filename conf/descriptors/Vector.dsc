#Vector
#Thu Sep 14 15:10:47 CEST 2017

db.name=Vector
db.desc=NCBI's Vector nucleotide database (BLAST)
db.type=n
db.ldir=${mirrordir}|n|Vector

db.files.include=vector.tar.gz
db.files.exclude=

tasks.global.post=delgz,deltar,makealias
tasks.unit.post=gunzip,untar

ftp.pswd=user@company.com
ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.rdir=/blast/db/
ftp.rdir.exclude=

depends=
history=0

