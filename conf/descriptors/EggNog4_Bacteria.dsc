db.name=EggNog4_Bacteria
db.desc=EggNog v4.0 Bacteria division only database (contains annotations). 
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog4_Bacteria

depends=EggNog4_Prepare

db.files.include=bacteria.dat
db.files.exclude=

tasks.unit.post=idxnog
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

local.rdir=${mirrorprepadir}|p|EggNog4_Prepare|download|EggNog4_Prepare

history=0