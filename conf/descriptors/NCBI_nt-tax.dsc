db.name=NCBI_nt-tax
db.desc=NCBI nt databank with taxonomy. Blast v4 bank format. Nucleotide sequences from several sources, including GenBank, RefSeq, TPA and PDB. Not non-redundant.
db.type=n
db.ldir=${mirrordir}|n|NCBI_nt-tax

db.files.include=^nt_v4.*\\d+\\.tar.gz$
db.files.exclude=

tasks.unit.post=gunzip,untar
tasks.global.post=delgz,deltar,makealias

ftp.server=ftp.ncbi.nih.gov
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/blast/db
ftp.rdir.exclude=

history=0

aspera.use=true
aspera.server=anonftp@ftp.ncbi.nlm.nih.gov
aspera.args=-k 1 -T -l 640M
