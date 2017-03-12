db.name=EggNog4_Terms
db.desc=Nested Orthologous Groups (NOG) terms from the EggNog4 database
db.type=d
db.ldir=${mirrorprepadir}|d|EggNog4_Terms

depends=EggNog4_Prepare

db.files.include=all.members.tar
db.files.exclude=

tasks.unit.post=idxdico(type=nog)
tasks.global.post=deltmpidx

local.rdir=${mirrorprepadir}|p|EggNog4_Prepare|download|EggNog4_Prepare

history=0