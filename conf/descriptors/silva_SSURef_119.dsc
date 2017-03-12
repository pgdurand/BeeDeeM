db.name=silva_SSURef_119
db.desc=Datasets of aligned small (16S/18S, SSU) ribosomal RNA (rRNA) sequences (no annotations). Update 119.
db.type=n
db.ldir=${mirrordir}|n|silva_SSURef_119

db.files.include=SILVA_119_SSURef_Nr99_tax_silva_trunc.fasta.gz
db.files.exclude=

tasks.unit.post=gunzip,idxfas
tasks.global.post=formatdb(lclid\=false;check\=true;nr\=true;silva\=true)

ftp.server=ftp.arb-silva.de
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/release_119/Exports
ftp.rdir.exclude=

history=0