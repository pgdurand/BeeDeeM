db.name=Refseq_protein_sequence
db.desc=Refseq complete Protein databank (no annotations).
db.type=p
db.ldir=${mirrordir}|p|Refseq_protein_sequence

db.files.include=^complete.*\\d+\\.protein.faa.gz$,^complete.nonredundant_protein.*\\d+\\.protein.faa.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/refseq/release/complete/
ftp.rdir.exclude=

history=0