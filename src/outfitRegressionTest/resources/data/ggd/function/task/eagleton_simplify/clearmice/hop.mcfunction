function ggd:task/eagleton_simplify/clearmice/pick_slot
scoreboard players set @s ggdMiceRefresh 0
advancement revoke @s only mainsys:test/ifinvchange
advancement grant @s only mainsys:test/ifinvchange
