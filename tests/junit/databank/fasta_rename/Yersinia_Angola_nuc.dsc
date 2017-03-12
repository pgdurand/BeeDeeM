#Yersinia_Angola_nuc
#Thu Aug 28 09:53:44 CEST 2014
db.files.include=^.*\\.ffn$
tasks.global.post=formatdb(lclid\=false;check\=true;nr\=true)
tasks.unit.post=idxfas(rename\=id[prefix\=YPA+first\=1])
ftp.pswd=user@company.com
ftp.uname=anonymous
ftp.port=21
ftp.server=ftp.ncbi.nih.gov
db.name=Yersinia_Angola_nuc
db.ldir=${mirrordir}|n|Yersinia_Angola_nuc
ftp.rdir.exclude=
ftp.rdir=/genbank/genomes/Bacteria/Yersinia_pestis_Angola_uid16067
history=0
db.files.exclude=
db.desc=Tutorial example to show how to install the Yersinia pestis Angola nucleome from GenBank/Genomes databank.
db.type=n
