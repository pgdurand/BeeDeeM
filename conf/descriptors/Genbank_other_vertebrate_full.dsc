db.name=Genbank_other_vertebrate_full
db.desc=Genbank: Other vertebrate sequence Division (no annotations).
db.type=n
db.ldir=${mirrordir}|n|Genbank_other_vertebrate_full

# For a description of Genbank , see the following URL
# ftp:#ftp.ncbi.nlm.nih.gov/genbank/release.notes/
# then locate the latest release, or have a look at
# http:#www.pubmedcentral.nih.gov/articlerender.fcgi?artid=1347519
db.files.include=^gbvrt.*\.seq.gz$
db.files.exclude=

tasks.unit.post=gunzip,idxgb
tasks.global.post=delgz,deltmpidx,formatdb(lclid=false;check=true;nr=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/genbank
ftp.rdir.exclude=

history=0

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M
