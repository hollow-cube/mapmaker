# Traits

Traits are an (unimplemented) idea for a way to significantly optimize syntax processing in most cases.

Basically instead of dynamic command permissions, you would assign a set of required traits to a command.
For example in mapmaker the `/test` command could require the `MAP_EDITOR` trait. These traits would be
explicitly assigned to the player, eg `player.addTrait(MAP_EDITOR)`. Once assigned commands would be resent
to the player matching the new traits.

The advantage here is that the server can very aggressively cache syntax trees for particular combinations
of traits. In all likelihood there would not be very many common combinations of traits, so this would make
refreshing commands a free-ish operation.
