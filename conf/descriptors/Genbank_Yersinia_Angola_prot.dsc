db.name=Genbank_Yersinia_Angola_prot
db.desc=Yersinia pestis Angola proteome from GenBank/Genomes databank (no annotations).
db.type=p
db.ldir=${mirrordir}|p|Genbank_Yersinia_Angola_prot

# For a description of Genbank , see the following URL
# ftp:#ftp.ncbi.nlm.nih.gov/genbank/release.notes/
# then locate the latest release, or have a look at
# http:#www.pubmedcentral.nih.gov/articlerender.fcgi?artid=1347519
db.files.include=^.*\\.faa$
db.files.exclude=

tasks.unit.post=
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/genbank/genomes/Bacteria/Yersinia_pestis_Angola.*
ftp.rdir.exclude=

history=0

