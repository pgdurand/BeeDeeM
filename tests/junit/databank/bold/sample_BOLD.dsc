db.name=Bold_Sample
db.desc=Bold Sample for tests
db.type=n
db.ldir=${mirrordir}|n|Bold_Sample
db.files.include=iBOL.zip
db.files.exclude=

tasks.unit.post=gunzip,bold2gb,idxgb

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/bold
ftp.rdir.exclude=

history=0

