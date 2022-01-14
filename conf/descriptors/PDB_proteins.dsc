#PDB_proteins

db.name=PDB_proteins
db.desc=PDB Protein databank with taxonomy
db.type=p
db.ldir=${mirrordir}|p|PDB_proteins

db.files.include=pdbaa.tar.gz
db.files.exclude=

tasks.global.post=delgz,makealias
tasks.unit.post=gunzip,untar

ftp.uname=anonymous
ftp.pswd=user@institute.org
#ftp.server=165.112.9.228
ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.rdir=/blast/db/
ftp.rdir.exclude=

depends=

history=0

aspera.use=false
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M

