db.name=EggNog3_Terms
db.desc=Nested Orthologous Groups (NOG) terms from the EggNog3 database
db.type=d
db.ldir=${mirrorprepadir}|d|EggNog3_Terms

depends=EggNog3_Prepare

db.files.include=all.members.tar
db.files.exclude=

tasks.unit.post=idxdico(type=nog)
tasks.global.post=deltmpidx

local.rdir=${mirrorprepadir}|p|EggNog3_Prepare|download|EggNog3_Prepare

history=0

