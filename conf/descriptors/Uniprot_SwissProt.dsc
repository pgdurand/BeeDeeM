db.name=Uniprot_SwissProt
db.desc=UniprotKB/SwissProt databank (contains annotations).
db.type=p
db.ldir=${mirrordir}|p|Uniprot_SwissProt

db.files.include=uniprot_sprot.dat.gz
db.files.exclude=

tasks.unit.post=gunzip,idxsw
tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true),script(name=GetUP;path=get_up_release.sh)

ftp.server=ftp.expasy.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/databases/uniprot/current_release/knowledgebase/complete
ftp.rdir.exclude=

history=0
