db.name=Enzyme
db.desc=Enzyme nomenclature
db.type=d
db.ldir=${mirrordir}|d|Enzyme

db.files.include=enzyme.dat,enzclass.txt
db.files.exclude=

tasks.unit.post=
tasks.global.post=idxdico(type=ecc;file=enzclass.txt),idxdico(type=ec;file=enzyme.dat),deltmpidx,script(name=GetEZ;path=get_enz_release)

ftp.server=ftp.expasy.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databases/enzyme
ftp.rdir.exclude=

history=1

