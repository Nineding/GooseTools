# Persistent objectives: records survive disconnects and reloads until cancelled or completed.
scoreboard objectives add ggdId dummy
scoreboard objectives add ggdReading dummy
scoreboard objectives add ggdReadTime dummy
scoreboard objectives add ggdReadY dummy
scoreboard objectives add ggdReadZ dummy
scoreboard objectives add ggdReadSlot dummy
function ggd:task/eagleton_simplify/reading/hide_records
execute in minecraft:overworld if score map gamesetting matches 11 if entity @e[type=marker,tag=reading] run function ggd:task/eagleton_simplify/reading/init_slots
