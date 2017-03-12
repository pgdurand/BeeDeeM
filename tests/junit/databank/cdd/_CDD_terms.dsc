db.name=CDD_terms
db.desc=Conserved Domains terms from CDD Database.
db.type=d
db.ldir=${mirrordir}|d|CDD_terms

db.files.include=/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/cdd/CDD_terms.gz
db.files.exclude=

tasks.unit.post=gunzip,untar,idxdico(type=cdd)
tasks.global.post=deltmpidx

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0