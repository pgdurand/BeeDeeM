db.name=Uniprot_Sample
db.desc=Uniprot sample for tests
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Sample
db.files.include=/home1/homedir4/perso/pgdurand/devel/BeeDeeM/tests/junit/databank/uniprot/uniprot.dat.tar
db.files.exclude=

tasks.unit.post=untar,idxsw

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0
