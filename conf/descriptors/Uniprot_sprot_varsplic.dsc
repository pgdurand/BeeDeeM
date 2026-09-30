db.name=Uniprot_Sprot_Varsplic
db.desc=Additional sequences of the UniProtKB/Swiss-Prot data set that represent all annotated splice variants
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Sprot_Varsplic
db.provider=Uniprot

db.files.include=uniprot_sprot_varsplic.fasta.gz
db.files.exclude=

#tasks.unit.post=gunzip,script(name=CtFas;path=count_fasta)
#tasks.global.post=formatdb(lclid=true;check=false;nr=false),delgz

tasks.unit.post=gunzip
tasks.global.post=script(name=BlastDB;path=run_makeblastdb),delgz

# https://www.uniprot.org/help/downloads
# https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/

ftp.server=ftp.uniprot.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/databases/uniprot/current_release/knowledgebase/complete/
ftp.rdir.exclude=

history=2
