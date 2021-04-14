db.name=SwissProt_rattus
db.desc=Rat subset of UniprotKB/SwissProt (contains annotations).
db.type=p
db.ldir=${mirrordir}|p|SwissProt_rattus

db.files.include=uniprot_sprot_rodents.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxsw(taxinc\=10114)
tasks.global.post=delgz,deltmpidx,formatdb(lclid\=false;check\=true;nr\=true;taxinc\=10114),script(name=GetUP;path=get_up_release.sh)

ftp.server=ftp.expasy.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databases/uniprot/current_release/knowledgebase/taxonomic_divisions
ftp.rdir.exclude=

history=0