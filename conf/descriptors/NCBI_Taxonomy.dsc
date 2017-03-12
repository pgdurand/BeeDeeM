db.name=NCBI_Taxonomy
db.desc=NCBI Taxonomy Scientific Terms and Tree Structure
db.type=d
db.ldir=${mirrordir}|d|NCBI_Taxonomy

db.files.include=taxdump.tar.gz
db.files.exclude=

tasks.unit.post=gunzip,untar,idxdico(type=tax; file=names.dmp),idxdico(type=nodes; file= nodes.dmp)
tasks.global.post=deltmpidx,deltar,delgz

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/taxonomy
ftp.rdir.exclude=

history=0

