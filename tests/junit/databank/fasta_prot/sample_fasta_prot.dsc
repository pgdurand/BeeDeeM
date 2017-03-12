db.name=Fasta_proteic_sample
db.desc=Fasta proteic sample for tests
db.type=p
db.ldir=${mirrordir}|p|Fasta_proteic_sample
db.files.include=uniprot.faa.tar
db.files.exclude=

tasks.unit.post=untar,idxfas

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/fasta_prot
ftp.rdir.exclude=

history=0

