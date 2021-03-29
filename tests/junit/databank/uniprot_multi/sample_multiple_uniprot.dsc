db.name=Uniprot_Sample_multiple
db.desc=Uniprot sample for tests
db.type=p
db.ldir=${mirrordir}|p|Uniprot_Sample_multiple
db.files.include=.*dat$
db.files.exclude=

tasks.unit.post=idxsw

tasks.global.post=formatdb(lclid=false;check=true;nr=true)

ftp.server=ftp.ifremer.fr
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/ifremer/dataref/bioinfo/sebimer/devel/beedeem/unit-tests/uniprot_multi
ftp.rdir.exclude=

history=0
