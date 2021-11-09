db.name=MEROPS_Peptidases
db.desc=Non-redundant library for all the peptidases and peptidase inhibitors included in MEROPS
db.type=p
db.ldir=${mirrordir}|p|MEROPS_Peptidases
db.provider=EBI

db.files.include=protease.lib
db.files.exclude=

tasks.unit.post=idxfas
tasks.global.post=deltmpidx,formatdb(lclid=false;check=true;nr=true)

ftp.server=ftp.ebi.ac.uk
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/databases/merops/current_release
ftp.rdir.exclude=

history=0


