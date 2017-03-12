db.name=Genbank_Patent
db.desc=Genbank Patent Division (contains annotations).
db.type=n
db.ldir=${mirrordir}|n|Genbank_Patent

db.files.include=^gbpat.*\\.seq.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxgb
tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/genbank
ftp.rdir.exclude=

history=0
