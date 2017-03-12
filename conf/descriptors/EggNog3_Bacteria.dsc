db.name=EggNog3_Bacteria
db.desc=EggNog v3.0 Bacteria division only database (contains annotations). 
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog3_Bacteria

depends=EggNog3_Prepare

db.files.include=bacteria.dat
db.files.exclude=

tasks.unit.post=idxnog
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

local.rdir=${mirrorprepadir}|p|EggNog3_Prepare|download|EggNog3_Prepare

history=0