db.name=Enzyme
db.desc=Enzyme
db.type=d
db.ldir=${mirrordir}|d|Enzyme
db.files.include=/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/RunningMirrorPanel/databanks/enzyme.dat, /Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/RunningMirrorPanel/databanks/enzclass.txt
db.files.exclude=

tasks.unit.post=
tasks.global.post=idxdico(type=ecc;file=enzclass.txt),idxdico(type=ec;file=enzyme.dat),deltmpidx

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

