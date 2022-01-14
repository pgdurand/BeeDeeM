db.name=NCBI_nt
db.desc=NCBI nt databank (Fasta, no taxonomy). Collection of sequences from several sources, including GenBank, RefSeq, TPA and PDB. Not non-redundant.
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

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M

history=0



