db.name=Fasta_seq_size_filter
db.desc=Fasta_seq_size_filter sample for tests
db.type=p
db.ldir=${mirrordir}|p|Fasta_seq_size_filter
db.files.include=uniprot.faa
db.files.exclude=

tasks.unit.post=idxfas(nr=true;seqsize=200to300)

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databank/fasta_prot
ftp.rdir.exclude=

history=0

