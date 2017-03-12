db.name=Silva_Sample
db.desc=Silva Sample for tests
db.type=n
db.ldir=${mirrordir}|n|Silva_Sample
db.files.include=silva.fasta.tar.gz
db.files.exclude=

tasks.unit.post=gunzip,untar,idxfas

tasks.global.post=formatdb(lclid=false;check=true;nr=true;silva=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/silva
ftp.rdir.exclude=

history=0

