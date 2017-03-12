db.name=redundant_sequences_sw
db.desc=redundant_sequences_sw sample for tests
db.type=p
db.ldir=${mirrordir}|p|redundant_sequences_sw
db.files.include=uniprot.dat
db.files.exclude=

tasks.unit.post=idxsw(nr=true)

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/uniprot
ftp.rdir.exclude=

history=0
