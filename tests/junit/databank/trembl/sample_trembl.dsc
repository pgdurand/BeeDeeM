db.name=Trembl_Sample
db.desc=Trembl sample for tests
db.type=p
db.ldir=${mirrordir}|p|Trembl_Sample
db.files.include=trembl.dat
db.files.exclude=

tasks.unit.post=idxsw

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/Trembl
ftp.rdir.exclude=

history=0
