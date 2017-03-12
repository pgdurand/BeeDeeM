db.name=NCBI_Taxonomy
db.desc=NCBI Taxonomy Scientific Terms and Tree Structure
db.type=d
db.ldir=${mirrordir}|d|NCBI_Taxonomy
db.files.include=testTaxonomy.zip
db.files.exclude=

tasks.unit.post=gunzip,idxdico(type=tax; file=names.dmp),idxdico(type=nodes; file= nodes.dmp)

tasks.global.post=deltmpidx,deltar,delgz

ftp.server=192.168.1.18
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/taxonomy
ftp.rdir.exclude=

history=0

