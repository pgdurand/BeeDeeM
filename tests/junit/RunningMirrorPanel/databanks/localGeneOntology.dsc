db.name=GeneOntology_terms
db.desc=Gene Ontology Terms
db.type=d
db.ldir=${mirrordir}|d|GeneOntology_terms

db.files.include=${local}/gene_ontology.obo
db.files.exclude=

tasks.unit.post=idxdico(type=go)
tasks.global.post=deltmpidx

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

