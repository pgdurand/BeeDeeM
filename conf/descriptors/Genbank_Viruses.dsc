db.name=Genbank_Viruses
db.desc=Install Genbank Virus (VRL) division database (contains annotations).
db.type=n
db.ldir=${mirrordir}|n|Genbank_Viruses

db.files.include=^gbvrl.*\\.seq.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxgb
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/genbank
ftp.rdir.exclude=

history=0



