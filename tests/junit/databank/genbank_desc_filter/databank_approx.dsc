db.name=Genbank_desc_filter_Sample
db.desc=Genbank_desc_filter for tests
db.type=n
db.ldir=${mirrordir}|n|Genbank_Sample
db.files.include=genbank.dat
db.files.exclude=

tasks.unit.post=idxgb(desc=maturose@isolatus;exactdesc=false)

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/genbank
ftp.rdir.exclude=

history=0

