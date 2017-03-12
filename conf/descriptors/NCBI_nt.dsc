db.name=NCBI_nt
db.desc=NCBI nt databank (no annotations). Collection of sequences from several sources, including GenBank, RefSeq, TPA and PDB. Not non-redundant.
db.type=n
db.ldir=${mirrordir}|n|NCBI_nt

db.files.include=^nt.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/blast/db/FASTA
ftp.rdir.exclude=

history=0



