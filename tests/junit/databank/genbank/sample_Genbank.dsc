db.name=Genbank_Sample
db.desc=Genbank sample for tests
db.type=n
db.ldir=${mirrordir}|n|Genbank_Sample
db.files.include=${local}/genbank.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxgb

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

