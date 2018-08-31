#PDB_proteins
#Fri Sep 15 13:47:28 CEST 2017
db.files.include=pdbaa.tar.gz
tasks.global.post=delgz,makealias
tasks.unit.post=gunzip,untar
ftp.pswd=user@institute.org
ftp.uname=anonymous
depends=
ftp.port=21
ftp.server=ftp.ncbi.nih.gov
db.name=PDB_proteins
db.ldir=${mirrordir}|p|PDB_proteins
ftp.rdir.exclude=
ftp.rdir=/blast/db/
history=0
db.desc=PDB Protein databank with taxonomy
db.files.exclude=
db.type=p
