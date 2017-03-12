db.name=Needs_uniprot_before
db.desc=Needs_uniprot_before
db.type=p
db.ldir=${mirrordir}|p|Needs_uniprot_before
db.files.include=Uniprot_Sample00
db.files.exclude=

depends=uniprotNoDepends

tasks.unit.post=idxfas
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

local.rdir=${mirrordir}|p|Uniprot_Sample|current|Uniprot_Sample

history=0
