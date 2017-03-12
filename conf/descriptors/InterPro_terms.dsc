db.name=InterPro_terms
db.desc=InterPro Terms
db.type=d
db.ldir=${mirrordir}|d|InterPro_terms
db.files.include=names.dat
db.files.exclude=

tasks.unit.post=idxdico(type=ipr)

tasks.global.post=deltmpidx

ftp.server=ftp.ebi.ac.uk
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/databases/interpro
ftp.rdir.exclude=

history=0
