db.name=Silva_LSU
db.desc=Silva LSU Reference sequence databank
db.type=n
db.ldir=${mirrordir}|n|Silva_LSU
db.provider=ARB

db.files.include=SILVA_138.1_LSURef_tax_silva.fasta.gz
db.files.exclude=

tasks.global.pre=script(name=GetSilvaLSU;path=get_silva_lsu)

tasks.unit.post=gunzip,idxfas
tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true),script(name=GetLSURelease;path=get_silva_release),script(name=BuildIndex;path=post_process_silva)

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

local.rdir=${workdir}|Silva_LSU
local.rdir.exclude=

history=0

