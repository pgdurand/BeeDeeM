db.name=NCBI_nr
db.desc=NCBI nr databank (no annotations). Contains non-identical sequences from GenBank CDS translations, PDB, Swiss-Prot, PIR, and PRF.
db.type=p
db.ldir=${mirrordir}|p|NCBI_nr

db.files.include=^nr.gz$
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


aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M

