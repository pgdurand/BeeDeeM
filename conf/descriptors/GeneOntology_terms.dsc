db.name=GeneOntology_terms
db.desc=Gene Ontology Terms
db.type=d
db.ldir=${mirrordir}|d|GeneOntology_terms

db.files.include=gene_ontology.obo
db.files.exclude=

tasks.unit.post=idxdico(type=go)
tasks.global.post=deltmpidx

ftp.server=ftp.geneontology.org
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/go/ontology
ftp.rdir.exclude=

history=0

