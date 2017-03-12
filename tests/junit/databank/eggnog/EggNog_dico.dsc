db.name=EggNog
db.desc=The EggNog classifications
db.type=d
db.ldir=${mirrorprepadir}|d|EggNog


db.files.include=${local}/eggnogv4.levels.txt
db.files.exclude=

tasks.unit.post=idxdico(type=eggnog)
tasks.global.post=

ftp.server=
ftp.port=
ftp.uname=
ftp.pswd=
ftp.rdir=
ftp.rdir.exclude=

history=0