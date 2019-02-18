db.name=Refseq_RNA_complete
db.desc=Refseq complete RNA databank (contains annotations).
db.type=n
db.ldir=${mirrordir}|n|Refseq_RNA_complete

db.files.include=^complete\\.\\d+\\.rna.gbff.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxgb
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)

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
