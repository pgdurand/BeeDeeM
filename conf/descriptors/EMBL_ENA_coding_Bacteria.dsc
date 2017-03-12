#EMBL_ENA_coding_Bacteria
#Fri Mar 20 14:59:00 CET 2015
db.files.include=^rel_std_pro.*.cds.gz$
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)
tasks.unit.post=gunzip,idxem
ftp.pswd=user@company.com
ftp.uname=anonymous
ftp.port=21
ftp.server=ftp.ebi.ac.uk
db.name=EMBL_ENA_coding_Bacteria
db.ldir=${mirrordir}|n|EMBL_ENA_coding_Bacteria
ftp.rdir.exclude=
ftp.rdir=/pub/databases/ena/coding/release/std/
history=0
db.files.exclude=
db.desc=EMBL Nucleotide Archive coding sequences, Bacteria division (contains annotations).
db.type=n
