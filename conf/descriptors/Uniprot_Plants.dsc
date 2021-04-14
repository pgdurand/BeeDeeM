db.name=Uniprot_Plants
db.desc=UniprotKB Plants division from SwissProt and TrEmbl (contains functional and taxonomy annotations).
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Plants
db.files.include=uniprot_sprot_plants.dat.gz,uniprot_trembl_plants.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxsw

tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true),script(name=GetUP;path=get_up_release.sh)

ftp.server=ftp.expasy.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databases/uniprot/current_release/knowledgebase/taxonomic_divisions
ftp.rdir.exclude=

history=0
