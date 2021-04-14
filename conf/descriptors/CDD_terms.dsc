db.name=CDD_terms
db.desc=Conserved Domains terms from CDD Database.
db.type=d
db.ldir=${mirrordir}|d|CDD_terms

db.files.include=^cddid.tbl.gz$
db.files.exclude=

tasks.unit.post=gunzip,untar,idxdico(type=cdd)
tasks.global.post=deltmpidx,script(name=GetCDD;path=get_cdd_release)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/mmdb/cdd
ftp.rdir.exclude=

history=0