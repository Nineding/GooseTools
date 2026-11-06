# Failed books are discarded. Only fill random empty slots when fewer than 40 remain.
execute unless score map gamesetting matches 11 run return 0
execute unless loaded -1581 74 -550 run return run schedule function ggd:task/eagleton_simplify/reading/refill 1t replace
execute unless loaded -1581 76 -542 run return run schedule function ggd:task/eagleton_simplify/reading/refill 1t replace
function ggd:task/eagleton_simplify/reading/scan_shelves
function ggd:task/eagleton_simplify/reading/fill_loop
kill @e[type=marker,tag=reading_empty]
