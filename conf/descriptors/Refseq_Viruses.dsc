db.name=Refseq_Viruses
db.desc=Refseq Virus (VRL) division database (contains annotations).
db.type=n
db.ldir=${mirrordir}|n|Refseq_Viruses

db.files.include=viral\\.\\d+\\.genomic.gbff.gz
db.files.exclude=

tasks.unit.post=gunzip,idxgb
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/refseq/release/viral/
ftp.rdir.exclude=

history=0