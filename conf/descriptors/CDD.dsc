db.name=CDD
db.desc=Conserved Domains Database (CDD): NCBI-curated protein functional domains (contains also domains from Pfam, SMART, COG, PRK, TIGRFAM). Data from: Marchler-Bauer A. et.al. (2015) NAR 43:D222-2.
db.type=p
db.ldir=${mirrordir}|p|CDD

db.files.include=^cddmasters.fa.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true;cdd\=true),script(name=GetCDD;path=get_cdd_release)

depends=CDD_terms

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/mmdb/cdd
ftp.rdir.exclude=

history=0