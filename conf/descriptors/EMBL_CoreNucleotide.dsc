db.name=EMBL_CoreNucleotide
db.desc=EMBL Core nucleotide database (contains annotations). STD EMBL division only.
db.type=n
db.ldir=${mirrordir}|n|EMBL_CoreNucleotide

#see ftp://ftp.ebi.ac.uk/pub/databases/embl/release/relnotes.txt
db.files.include=^rel_std_fun.*\\.dat.gz$,^rel_std_hum.*\\.dat.gz$,^rel_std_inv.*\\.dat.gz$,^rel_std_mam.*\\.dat.gz$,^rel_std_mus.*\\.dat.gz$,^rel_std_phg.*\\.dat.gz$,^rel_std_pln.*\\.dat.gz$,^rel_std_pro.*\\.dat.gz$,^rel_std_rod.*\\.dat.gz$,^rel_std_syn.*\\.dat.gz$,^rel_std_tgn.*\\.dat.gz$,^rel_std_unc.*\\.dat.gz$,^rel_std_vrl.*\\.dat.gz$,^rel_std_vrt.*\\.dat.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxem

tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true)

ftp.server=ftp.ebi.ac.uk
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/databases/embl/release/std/
ftp.rdir.exclude=

history=0

