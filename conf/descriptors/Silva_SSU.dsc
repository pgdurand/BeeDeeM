db.name=Silva_SSU
db.desc=Silva SSU Reference sequence databank
db.type=n
db.ldir=${mirrordir}|n|Silva_SSU
db.provider=ARB

db.files.include=SILVA_138.1_SSURef_tax_silva.fasta.gz
db.files.exclude=

tasks.global.pre=script(name=GetSilvaSSU;path=get_silva_ssu)

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true),script(name=GetSSURelease;path=get_silva_release),script(name=BuildIndex;path=post_process_silva)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

local.rdir=${workdir}|Silva_SSU
local.rdir.exclude=

history=0

