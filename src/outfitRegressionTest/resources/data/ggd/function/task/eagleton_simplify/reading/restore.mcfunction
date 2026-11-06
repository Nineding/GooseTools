# Cancellation/failure/disconnect discards this loan, then checks the 40-book floor.
execute store result storage ggd:reading args.owner int 1 run scoreboard players get @s ggdId
function ggd:task/eagleton_simplify/reading/discard_owner with storage ggd:reading args
kill @s
function ggd:task/eagleton_simplify/reading/refill
