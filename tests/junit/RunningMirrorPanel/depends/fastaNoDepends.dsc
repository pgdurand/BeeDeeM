db.name=Fasta_proteic_sample
db.desc=Fasta proteic sample for tests
db.type=p
db.ldir=${mirrordir}|p|Fasta_proteic_sample
db.files.include=${local}/uniprot.fasta
db.files.exclude=

tasks.unit.post=idxfas
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

