#SW_human_local
#Tue Mar 18 14:44:19 CET 2014
db.files.include=SwissProt_human.fas
tasks.global.post=formatdb(lclid\=false;check\=true;nr\=true)
tasks.unit.post=idxfas
ftp.pswd=user@institute.org
ftp.uname=anonymous
ftp.port=21
ftp.server=192.168.1.18
db.name=SW_human_local
db.ldir=${mirrordir}|p|SW_human_local
ftp.rdir.exclude=
ftp.rdir=/FASTA/
history=0
db.files.exclude=
db.desc=SW human Protein databank (Klast/Blast only).
db.type=p
