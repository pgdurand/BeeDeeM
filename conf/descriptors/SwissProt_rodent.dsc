db.name=SwissProt_rodent
db.desc=Rodent subset of UniprotKB/SwissProt (contains annotations).
db.type=p
db.ldir=${mirrordir}|p|SwissProt_rodent

db.files.include=uniprot_sprot_rodents.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxsw
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true),script(name=GetUP;path=get_up_release)

ftp.server=ftp.uniprot.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions
ftp.rdir.exclude=

history=0