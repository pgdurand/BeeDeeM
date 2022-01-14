db.name=PDB_proteins_fas
db.desc=PDB Protein databank (Fasta, no taxonomy).
db.type=p
db.ldir=${mirrordir}|p|PDB_proteins_fas

db.files.include=pdbaa.gz
db.files.exclude=

tasks.unit.post=gunzip,idxfas
tasks.global.post=formatdb(lclid\=true;check\=true;nr\=true),delgz


ftp.server=ftp.ncbi.nih.gov
ftp.uname=anonymous
ftp.pswd=user@institute.org
ftp.port=21
ftp.rdir.exclude=
ftp.rdir=/blast/db/FASTA/

history=0
depends=
