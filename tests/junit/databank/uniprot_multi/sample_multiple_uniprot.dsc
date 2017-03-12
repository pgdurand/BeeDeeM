db.name=Uniprot_Sample_multiple
db.desc=Uniprot sample for tests
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Sample_multiple
db.files.include=.*dat$
db.files.exclude=

tasks.unit.post=idxsw

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/unittests/uniprot_multi
ftp.rdir.exclude=

history=0
