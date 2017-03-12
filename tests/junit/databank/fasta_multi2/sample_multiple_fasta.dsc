db.name=Fasta_Sample_multiple2
db.desc=Fasta sample for tests
db.type=p
db.ldir=${mirrordir}|p|Fasta_Sample_multiple2
db.files.include=.*faa$
db.files.exclude=

tasks.unit.post=idxfas

tasks.global.post=formatdb(lclid=false;check=false;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/uniprot
ftp.rdir.exclude=

history=0
