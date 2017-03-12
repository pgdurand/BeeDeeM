db.name=Uniprot_Sample
db.desc=Uniprot sample for tests
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Sample
db.files.include=/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/RunningMirrorPanel/depends/uniprot.dat
db.files.exclude=

depends=fastaDependsUniprot

tasks.unit.post=idxsw
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0
