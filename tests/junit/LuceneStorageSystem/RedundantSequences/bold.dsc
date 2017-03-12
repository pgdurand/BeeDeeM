db.name=redundant_sequences_bold
db.desc=redundant_sequences_bold sample for tests
db.type=p
db.ldir=${mirrordir}|p|redundant_sequences_bold
db.files.include=iBOL.zip
db.files.exclude=

tasks.unit.post=gunzip,bold2gb,idxgb

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/uniprot
ftp.rdir.exclude=

history=0
