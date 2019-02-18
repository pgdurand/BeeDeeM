db.name=Genbank_CoreNucleotide_blast
db.desc=Genbank Core nucleotide database (no annotations). All Genbank but EST, STS, GSS, HTG, HTC, CON and ENV.
db.type=n
db.ldir=${mirrordir}|n|Genbank_CoreNucleotide_blast

db.files.include=^nt.*\\.gz$
db.files.exclude=

tasks.unit.post=gunzip,untar,idxfas

tasks.global.post=delgz,makealias

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/blast/db/
ftp.rdir.exclude=

history=0

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M
