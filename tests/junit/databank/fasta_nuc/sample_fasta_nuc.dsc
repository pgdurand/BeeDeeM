db.name=Fasta_nucleic_sample
db.desc=Fasta nucleic sample for tests
db.type=n
db.ldir=${mirrordir}|n|Fasta_nucleic_sample
db.files.include=genbank.fas
db.files.exclude=

tasks.unit.post=idxfas

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/fasta_nuc
ftp.rdir.exclude=

history=0

