db.name=GeneOntology_terms
db.desc=Gene Ontology Terms
db.type=d
db.ldir=${mirrordir}|d|GeneOntology_terms

db.files.include=go-basic.obo
db.files.exclude=

tasks.global.pre=script(name=GetGO;path=get_gene_ontology.sh)

tasks.unit.post=idxdico(type=go)
tasks.global.post=deltmpidx,script(name=GetGO;path=get_go_release.sh)

local.rdir=${workdir}|GeneOntology_terms
local.rdir.exclude=

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

