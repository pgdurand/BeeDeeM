db.name=EggNog_prepare
db.desc=
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog_prepare

db.files.include=/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/eggnog/eggnogv4.proteins.all.fa.gz,/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/eggnog/eggnogv4.levels.txt,/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/eggnog/eggnogv4.funccats.txt,/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/eggnog/all.members.tar.gz,/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/eggnog/all.description.tar.gz,/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/eggnog/all.funccat.tar.gz
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