db.name=EMBL_Sample
db.desc=EMBL Sample for tests
db.type=n
db.ldir=${mirrordir}|n|EMBL_Sample
db.files.include=embl.dat
db.files.exclude=

tasks.unit.post=idxem

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/embl
ftp.rdir.exclude=

history=0

