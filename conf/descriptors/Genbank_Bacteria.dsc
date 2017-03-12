db.name=Genbank_Bacteria
db.desc=Genbank Bacteria (BCT) division only database (contains annotations). 
db.type=n
db.ldir=${mirrordir}|n|Genbank_Bacteria

db.files.include=^gbbct.*\\.seq.gz$
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

