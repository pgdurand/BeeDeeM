db.name=Pfam-hmm
db.desc=Large collection of protein families, each represented by multiple sequence alignments and hidden Markov models (HMMs)
db.type=d
db.ldir=${mirrordir}|d|Pfam-hmm

db.files.include=Pfam-A.hmm.gz
db.files.exclude=

tasks.unit.post=gunzip
# by default, post-process script is located in ../scripts path
tasks.global.post=delgz,script(name=hmmpress;path=pfam-hmm.sh)

ftp.server=ftp.ebi.ac.uk
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/databases/Pfam/current_release
ftp.rdir.exclude=

history=0
