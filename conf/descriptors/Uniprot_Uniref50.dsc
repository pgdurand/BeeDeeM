db.name=Uniprot_Uniref50
db.desc=Uniprot TrEMBL/SwissProt non-redundant, all organisms from UniProt/Uniref 50 databank (no annotations).
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Uniref50

db.files.include=uniref50.fasta.gz
db.files.exclude=

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true),script(name=GetUP;path=get_up_release)

ftp.server=ftp.expasy.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databases/uniprot/current_release/uniref/uniref50
ftp.rdir.exclude=

history=0
