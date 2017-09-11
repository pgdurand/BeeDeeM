db.name=redundant_sequences_gb
db.desc=redundant_sequences_gb sample for tests
db.type=n
db.ldir=${mirrordir}|n|redundant_sequences_gb
db.files.include=genbank.dat
db.files.exclude=

tasks.unit.post=idxgb(nr=true)

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0
