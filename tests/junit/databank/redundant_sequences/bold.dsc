db.name=redundant_sequences_bold
db.desc=redundant_sequences_bold sample for tests
db.type=n
db.ldir=${mirrordir}|n|redundant_sequences_bold
db.files.include=iBOL.zip
db.files.exclude=

tasks.unit.post=gunzip,bold2gb,idxgb

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0
