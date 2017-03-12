db.name=EggNog3_Archea
db.desc=EggNog v3.0 Archea division only database (contains annotations). 
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog3_Archea

depends=EggNog3_Prepare

db.files.include=archea.dat
db.files.exclude=

tasks.unit.post=idxnog
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

local.rdir=${mirrorprepadir}|p|EggNog3_Prepare|download|EggNog3_Prepare


history=0