db.name=Uniprot_Sample
db.desc=Uniprot sample for tests
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Sample
db.files.include=uniprot.dat.tar
db.files.exclude=

tasks.unit.post=untar,idxsw

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/uniprot
ftp.rdir.exclude=

history=0
