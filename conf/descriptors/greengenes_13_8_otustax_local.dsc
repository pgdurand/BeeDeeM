db.name=greengenes_13_8_otus
db.desc=Greengenes taxonomy dedicated to Bacteria and Archaea.
db.type=n
db.ldir=${mirrordir}|n|greengenes_13_8_otus

db.files.include=greengenes13.8.fasta
db.files.exclude=

tasks.unit.post=
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

local.rdir=/home/ref-bioinfo/beedeem/n/greengenes_13_8_otustax/download/
local.rdir.exclude=

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

