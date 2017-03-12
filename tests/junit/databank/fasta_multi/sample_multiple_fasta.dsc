db.name=Fasta_Sample_multiple
db.desc=Fasta sample for tests
db.type=p
db.ldir=${mirrordir}|p|Fasta_Sample_multiple
db.files.include=.*faa$
db.files.exclude=

tasks.unit.post=idxfas(nr=true)

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/uniprot
ftp.rdir.exclude=

history=0
