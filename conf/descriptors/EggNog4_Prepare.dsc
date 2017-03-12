db.name=EggNog4_Prepare
db.desc=
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog4_Prepare

db.files.include=eggnogv4.proteins.all.fa.gz,eggnogv4.levels.txt,eggnogv4.funccats.txt,all.members.tar.gz,all.description.tar.gz,all.funccat.tar.gz
db.files.exclude=

tasks.unit.post=gunzip
tasks.global.post=eggnog(members=all.members.tar;descriptions=all.description.tar;funccats=all.funccat.tar;sequences=eggnogv4.proteins.all.fa),noiip

ftp.server=eggnog.embl.de
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/eggNOG/4.0,/eggNOG/4.0/members,/eggNOG/4.0/description,/eggNOG/4.0/funccat
ftp.rdir.exclude=

history=0