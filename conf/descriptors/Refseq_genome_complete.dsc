db.name=Refseq_genome_complete
db.desc=Refseq complete Genome sequence databank (contains annotations).
db.type=n
db.ldir=${mirrordir}|n|Refseq_genome_complete

db.files.include=^complete.*\\d+\\.genomic.gbff.gz$
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