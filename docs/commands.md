# Commands

## dig
Main command of MelonScoop.  

Here the subcommands of the command `dig`:
 + player  
 + ip  

### player
Usage: `/dig player <username>`  
Permission needed:  `melonscoop.dig`  

This command allows you to query all the IP addresses used by a player.  

If an IP address was used by more than one players, it will be shown as a yellow line in the result list, and all the player uses this IP address and the player status(if a player was banned) will be listed.

### ip
Usage: `/dig ip <address>`  
Permission needed:  `melonscoop.dig`  

This command allows you to look up all the players served by specified IP address.  

If more than one players were related to the specified IP address, the result will be shown as yellow. And all the player served by this IP address and their status(if a player was banned) will be listed.