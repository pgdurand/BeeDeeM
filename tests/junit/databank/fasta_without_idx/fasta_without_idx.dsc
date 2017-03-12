db.name=Fasta_no_index_sample
db.desc=Fasta without index sample for tests
db.type=p
db.ldir=${mirrordir}|p|Fasta_no_index
db.files.include=SwissProt_human.fas
db.files.exclude=

tasks.unit.post=

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/fasta_without_idx
ftp.rdir.exclude=

history=0
