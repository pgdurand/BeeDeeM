#Genbank_Yersinia_Angola_prot
#Tue Oct 31 09:26:56 CET 2017
db.files.include=GCF_000009065.1_ASM906v1_protein.faa.gz
tasks.global.post=formatdb(lclid\=false;check\=true;nr\=true)
tasks.unit.post=gunzip,idxfas
ftp.pswd=user@company.com
ftp.uname=anonymous
depends=
ftp.port=21
ftp.server=ftp.ncbi.nih.gov
db.name=Genbank_Yersinia_Angola_prot
db.ldir=${mirrordir}|p|Genbank_Yersinia_Angola_prot
ftp.rdir.exclude=
ftp.rdir=/genomes/all/GCF/000/009/065/GCF_000009065.1_ASM906v1
history=0
db.desc=Yersinia pestis Angola proteome from GenBank/Genomes databank (no annotations).
db.files.exclude=
db.type=p
