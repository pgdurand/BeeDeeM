#PDB_proteins
#Sun Jul 31 17:23:32 CEST 2016
db.files.include=pdbaa.gz
tasks.global.post=formatdb(lclid\=true;check\=true;nr\=true)
tasks.unit.post=gunzip,idxfas
ftp.pswd=user@institute.org
ftp.uname=anonymous
depends=
ftp.port=21
ftp.server=ftp.ncbi.nih.gov
db.name=PDB_proteins
db.ldir=${mirrordir}|p|PDB_proteins
ftp.rdir.exclude=
ftp.rdir=/blast/db/FASTA/
history=0
db.desc=PDB Protein databank\: sequences from 3D protein structures (no annotations).
db.files.exclude=
db.type=p
