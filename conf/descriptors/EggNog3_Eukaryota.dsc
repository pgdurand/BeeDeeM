db.name=EggNog3_Eukaryota
db.desc=EggNog v3.0 Eukaryota division only database (contains annotations). 
db.type=p
db.ldir=${mirrorprepadir}|p|EggNog3_Eukaryota

depends=EggNog3_Prepare

db.files.include=eukaryota.dat
db.files.exclude=

tasks.unit.post=idxnog
tasks.global.post=formatdb(lclid=false;check=true;nr=true)

local.rdir=${mirrorprepadir}|p|EggNog3_Prepare|download|EggNog3_Prepare

history=0