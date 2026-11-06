$execute if items block -1581 $(y) $(z) container.0 * run scoreboard players add #Books ggdReading 1
$execute unless items block -1581 $(y) $(z) container.0 * run function ggd:task/eagleton_simplify/reading/empty_slot {y:$(y),z:$(z),slot:0}
$execute if items block -1581 $(y) $(z) container.1 * run scoreboard players add #Books ggdReading 1
$execute unless items block -1581 $(y) $(z) container.1 * run function ggd:task/eagleton_simplify/reading/empty_slot {y:$(y),z:$(z),slot:1}
$execute if items block -1581 $(y) $(z) container.2 * run scoreboard players add #Books ggdReading 1
$execute unless items block -1581 $(y) $(z) container.2 * run function ggd:task/eagleton_simplify/reading/empty_slot {y:$(y),z:$(z),slot:2}
$execute if items block -1581 $(y) $(z) container.3 * run scoreboard players add #Books ggdReading 1
$execute unless items block -1581 $(y) $(z) container.3 * run function ggd:task/eagleton_simplify/reading/empty_slot {y:$(y),z:$(z),slot:3}
$execute if items block -1581 $(y) $(z) container.4 * run scoreboard players add #Books ggdReading 1
$execute unless items block -1581 $(y) $(z) container.4 * run function ggd:task/eagleton_simplify/reading/empty_slot {y:$(y),z:$(z),slot:4}
$execute if items block -1581 $(y) $(z) container.5 * run scoreboard players add #Books ggdReading 1
$execute unless items block -1581 $(y) $(z) container.5 * run function ggd:task/eagleton_simplify/reading/empty_slot {y:$(y),z:$(z),slot:5}
$execute if items block -1581 $(y) $(z) container.0 * run item modify block -1581 $(y) $(z) container.0 ggd:reading_shelf
$execute if items block -1581 $(y) $(z) container.1 * run item modify block -1581 $(y) $(z) container.1 ggd:reading_shelf
$execute if items block -1581 $(y) $(z) container.2 * run item modify block -1581 $(y) $(z) container.2 ggd:reading_shelf
$execute if items block -1581 $(y) $(z) container.3 * run item modify block -1581 $(y) $(z) container.3 ggd:reading_shelf
$execute if items block -1581 $(y) $(z) container.4 * run item modify block -1581 $(y) $(z) container.4 ggd:reading_shelf
$execute if items block -1581 $(y) $(z) container.5 * run item modify block -1581 $(y) $(z) container.5 ggd:reading_shelf
