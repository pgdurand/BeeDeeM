db.name=redundant_sequences
db.desc=redundant_sequences for tests
db.type=n
db.ldir=${mirrordir}|n|redundant_sequences
db.files.include=uniprot.faa
db.files.exclude=

tasks.unit.post=

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/silva
ftp.rdir.exclude=

history=0

