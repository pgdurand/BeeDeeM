db.name=Refseq_protein_viridiplantae
db.desc=Refseq Viridiplantae Protein databank (contains annotations).
db.type=p
db.ldir=${mirrordir}|p|Refseq_protein_viridiplantae

db.files.include=^plant\\.\\d+\\.protein.gpff.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxgp
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/refseq/release/plant/
ftp.rdir.exclude=

history=0

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M
