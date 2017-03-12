db.name=CDD
db.desc=Conserved Domains Database (CDD)\: NCBI-curated protein functional domains (contains also domains from Pfam, SMART, COG, PRK, TIGRFAM). Data from\: Marchler-Bauer A. et.al. (2015) NAR 43\:D222-2.
db.type=p
db.ldir=${mirrordir}|p|CDD

db.files.include=/Users/pdurand/Documents/workspace_kepler/P-bioinfo-dbms/tests/junit/databank/cdd/cddmasters.fa
db.files.exclude=

tasks.unit.post=idxfas
tasks.global.post=deltmpidx,formatdb(lclid=false;cdd=true)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

