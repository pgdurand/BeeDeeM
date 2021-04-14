db.name=SwissProt_human
db.desc=Human subset of UniprotKB/SwissProt (contains annotations).
db.type=p
db.ldir=${mirrordir}|p|SwissProt_human

db.files.include=uniprot_sprot_human.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxsw
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true),script(name=GetUP;path=get_up_release)

ftp.server=ftp.expasy.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databases/uniprot/current_release/knowledgebase/taxonomic_divisions
ftp.rdir.exclude=

history=0