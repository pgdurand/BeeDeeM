db.name=InterPro_terms
db.desc=InterPro Terms
db.type=d
db.ldir=${mirrordir}|d|InterPro_terms
db.files.include=${local}|names.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxdico(type=ipr)

tasks.global.post=deltmpidx

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

