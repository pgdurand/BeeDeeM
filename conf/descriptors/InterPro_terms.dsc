db.name=InterPro_terms
db.desc=InterPro Terms
db.type=d
db.ldir=${mirrordir}|d|InterPro_terms
db.files.include=names.dat
db.files.exclude=
db.provider=EBI

tasks.unit.post=idxdico(type=ipr),script(name=GetIP;path=get_ip_release)

tasks.global.post=deltmpidx

ftp.server=ftp.ebi.ac.uk
ftp.port=21
ftp.uname=anonymous
ftp.pswd=user@company.com
ftp.rdir=/pub/databases/interpro/current_release
ftp.rdir.exclude=

history=0
