db.name=Fasta_with_taxon
db.desc=Fasta with taxon sample for tests
db.type=p
db.ldir=${mirrordir}|p|Fasta_with_taxon
db.files.include=microB_10seq.fas
db.files.exclude=

tasks.unit.post=idxfas

tasks.global.post=formatdb(lclid=false;check=true;nr=true;taxonomy\=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/fasta_without_idx
ftp.rdir.exclude=

history=0
