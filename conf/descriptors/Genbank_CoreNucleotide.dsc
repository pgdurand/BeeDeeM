db.name=Genbank_CoreNucleotide
db.desc=Genbank Core nucleotide database (contains annotations). All Genbank but EST, STS, GSS, HTG, HTC, CON and ENV.
db.type=n
db.ldir=${mirrordir}|n|Genbank_CoreNucleotide

# For a description of Genbank , see the following URL
# ftp:#ftp.ncbi.nlm.nih.gov/genbank/release.notes/
# then locate the latest release, or have a look at
# http:#www.pubmedcentral.nih.gov/articlerender.fcgi?artid=1347519
db.files.include=^gb.*\\.seq.gz$
db.files.exclude=^gbest.*\\.seq.gz$,^gbgss.*\\.seq.gz$,^gbhtg.*\\.seq.gz$,^gbhtc.*\\.seq.gz$,^gbsts.*\\.seq.gz$,^gbcon.*\\.seq.gz$,^gbenv.*\\.seq.gz$

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
