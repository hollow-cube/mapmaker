# Terraform
Terraform is a set of world manipulation tools. It is designed as a Minestom-native alternative to WorldEdit, VoxelSniper, etc.

## Concepts

### Selection
In Terraform, each player may have multiple different selections, each which can be configured independently (eg shape, masking, etc).
This will allow compatibility layers such as Loft in Arceon to continue functioning as if they have their own selection, while still
using the core terraform selection implementation to maintain compatibility with other settings.

For every player, there is a default selection. This is used in WorldEdit compatibility when executing commands such as `//set`, `//replace`, etc.
Other compatibility layers which use their own selection 

Tools may be assigned to specific selections, ... todo these details when i get to them


### World vs Global
Clipboard _MIGHT_ eventually be global, but can be per world right now

