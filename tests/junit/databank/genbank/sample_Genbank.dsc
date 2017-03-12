db.name=Genbank_Sample
db.desc=Genbank sample for tests
db.type=n
db.ldir=${mirrordir}|n|Genbank_Sample
db.files.include=genbank.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxgb

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/genbank
ftp.rdir.exclude=

history=0

