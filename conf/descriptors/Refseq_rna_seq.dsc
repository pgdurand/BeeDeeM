db.name=Refseq_rna_seq
db.desc=Refseq complete RNA databank (no annotations).
db.type=n
db.ldir=${mirrordir}|n|Refseq_rna_seq

db.files.include=^complete\\.\\d+\\.rna.fna.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,formatdb(lclid=false;check=true;nr=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/refseq/release/complete/
ftp.rdir.exclude=

history=0

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M
