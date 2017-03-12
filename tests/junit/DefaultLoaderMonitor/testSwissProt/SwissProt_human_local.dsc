#SwissProt_human_local
#Wed Mar 19 13:55:50 CET 2014
db.files.include=uniprot_sprot_human.dat.gz
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true)
tasks.unit.post=gunzip,idxsw
ftp.pswd=user@company.com
ftp.uname=anonymous
ftp.port=21
ftp.server=192.168.1.18
db.name=SwissProt_human_local
db.ldir=${mirrordir}|p|SwissProt_human_local
ftp.rdir.exclude=
ftp.rdir=/FASTA/
history=0
db.files.exclude=
db.desc=Tutorial example to show how to create a Human subset of UniprotKB/SwissProt\: data + Klast/Blast banks
db.type=p
