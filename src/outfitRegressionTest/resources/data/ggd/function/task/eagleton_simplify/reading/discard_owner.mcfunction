$clear @a *[custom_data~{readingLoan:1b,readingOwner:$(owner)}]
$execute as @e[type=item] if items entity @s contents *[custom_data~{readingLoan:1b,readingOwner:$(owner)}] run kill @s
execute as @e[type=marker,tag=reading_book_slot] run function ggd:task/eagleton_simplify/reading/discard_owner_book
