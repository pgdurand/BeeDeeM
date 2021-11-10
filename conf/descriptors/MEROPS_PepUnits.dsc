db.name=MEROPS_Pep_Units
db.desc=Non-redundant library of peptidase units and inhibitor units of all the peptidases and peptidase inhibitors that are included in MEROPS
db.type=p
db.ldir=${mirrordir}|p|MEROPS_Pep_Units
db.provider=EBI

db.files.include=pepunit.lib
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


