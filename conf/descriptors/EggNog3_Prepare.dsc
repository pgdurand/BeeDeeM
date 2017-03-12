db.name=EggNog3_Prepare
db.desc=
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog3_Prepare

db.files.include=protein.sequences.v3.fa.gz,levels.txt,fun.txt.gz,all.members.tar.gz,all.description.tar.gz,all.funccat.tar.gz
db.files.exclude=

tasks.unit.post=gunzip
tasks.global.post=eggnog(members=all.members.tar;descriptions=all.description.tar;funccats=all.funccat.tar;sequences=protein.sequences.v3.fa),noiip

ftp.server=eggnog.embl.de
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/eggNOG/3.0
ftp.rdir.exclude=

history=0