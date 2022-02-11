db.name=Refseq_protein_subset
db.desc=Refseq protein subset (contains annotations).
db.type=p
db.ldir=${mirrordir}|p|Refseq_protein_subset

db.files.include=complete.2337.protein.gpff.gz,complete.2338.protein.gpff.gz,complete.2339.protein.gpff.gz
db.files.exclude=

tasks.unit.post=gunzip,idxgp
tasks.global.post=formatdb(lclid\=false;check\=true;nr\=true),script(name=GetREF;path=get_ref_release),delgz,deltmpidx

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/refseq/release/complete/
ftp.rdir.exclude=
ftp.alt.protocol=https

history=0

aspera.use=false
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M
