db.name=Refseq_viral_protein
db.desc=Refseq Viral Division Protein (contains annotations).
db.type=p
db.ldir=${mirrordir}|p|Refseq_viral_protein

db.files.include=^viral.*\\.protein.gpff.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxgp
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true),script(name=GetREF;path=get_ref_release)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/refseq/release/viral/
ftp.rdir.exclude=

history=0

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M
