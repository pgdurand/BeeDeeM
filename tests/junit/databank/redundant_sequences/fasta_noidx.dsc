db.name=redundant_sequences
db.desc=redundant_sequences for tests
db.type=p
db.ldir=${mirrordir}|p|redundant_sequences
db.files.include=uniprot.faa
db.files.exclude=

tasks.unit.post=

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

