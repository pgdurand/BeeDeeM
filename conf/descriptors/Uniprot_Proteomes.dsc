db.name=Uniprot_Reference_Proteomes
db.desc=UniprotKB/Reference Proteomes, all sequences
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Reference_Proteomes
db.provider=EBI

db.files.include=.*_volume.fasta.gz$
db.files.exclude=

#tasks.global.pre=script(name=GetRefProteomes;path=get_up_proteomes)

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true),script(name=GetUP;path=get_up_proteomes_release),script(name=DiamondIndex;path=make_diamond_idx)

local.rdir=${workdir}|Uniprot_Reference_Proteomes
local.rdir.exclude=

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0


