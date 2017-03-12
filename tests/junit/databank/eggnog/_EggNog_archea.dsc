db.name=EggNog_archea
db.desc=The EggNog_archea
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog_archea

local.rdir=${mirrorprepadir}|p|EggNog_prepare|download|EggNog_prepare
db.files.include=all.dat
db.files.exclude=

tasks.unit.post=idxnog
tasks.global.post=formatdb(lclid=false;check=true;nr=true)


history=0