db.name=GeneOntology_terms
db.desc=Gene Ontology Terms
db.type=d
db.ldir=${mirrordir}|d|GeneOntology_terms

db.files.include=${local}|go-basic.obo.gz
db.files.exclude=

tasks.unit.post=gunzip,idxdico(type=go)
tasks.global.post=deltmpidx,delgz

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0

