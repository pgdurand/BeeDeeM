db.name=Listeria_proteins_sample
db.desc=Listeria_proteins sample for tests
db.type=p
db.ldir=${mirrordir}|p|Listeria_proteins_sample
db.files.include=Listeria_proteins.fasta
db.files.exclude=

tasks.unit.post=idxfas

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

history=0

