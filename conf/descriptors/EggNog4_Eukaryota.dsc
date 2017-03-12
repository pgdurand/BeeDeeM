db.name=EggNog4_Eukaryota
db.desc=EggNog v4.0 Eukaryota division only database (contains annotations). 
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog4_Eukaryota

depends=EggNog4_Prepare

db.files.include=eukaryota.dat
db.files.exclude=

tasks.unit.post=idxnog
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

local.rdir=${mirrorprepadir}|p|EggNog4_Prepare|download|EggNog4_Prepare

history=0