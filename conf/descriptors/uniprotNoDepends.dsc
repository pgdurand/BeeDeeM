db.name=Uniprot_Sample
db.desc=Uniprot sample for tests
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Sample
db.files.include=/Users/pgdurand/devel/plealog/BeeDeeM/tests/junit/RunningMirrorPanel/depends/uniprot.dat
db.files.exclude=

tasks.unit.post=idxsw

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0
