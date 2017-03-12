db.name=EggNog_prepare
db.desc=
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog_prepare

db.files.include=${local}/eggnogv4.proteins.all.fa.gz,${local}/eggnogv4.levels.txt,${local}/eggnogv4.funccats.txt,${local}/all.members.tar.gz,${local}/all.description.tar.gz,${local}/all.funccat.tar.gz
db.files.exclude=

tasks.unit.post=gunzip
tasks.global.post=eggnog(members=all.members.tar;descriptions=all.description.tar;funccats=all.funccat.tar;sequences=eggnogv4.proteins.all.fa),noiip

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0