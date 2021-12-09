db.name=Enzyme
db.desc=Enzyme nomenclature
db.type=d
db.ldir=${mirrordir}|d|Enzyme

db.files.include=${local}|enzyme.tar.gz
db.files.exclude=

tasks.unit.post=gunzip,untar,idxdico(type=ecc;file=enzclass.txt),idxdico(type=ec;file=enzyme.dat)
tasks.global.post=deltmpidx,deltar,delgz

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

