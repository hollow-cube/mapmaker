# Cosmetic Generator (v2)

This works slightly differently to the other generators. Instead of searching for `{name}.json5` files, it searches
for directories in `cosmeticv2/{type}/{name}`. The directory name describes the type of cosmetic, and expects the
following entries in the directory:

- `icon.png`: 16x16 2d icon for the cosmetic.
- `model.json`: Minecraft item model file
- `model.png`: Minecraft item texture file

The generator will create the following sprites (eg for use with `BadSprite`):
- `cosmetic/{type}/{name}/icon`: An item icon sprite for the icon
- `cosmetic/{type}/{name}/icon_locked`: An item icon sprite for the icon with the lock overlay
- `cosmetic/{type}/{name}`: An item model for the cosmetic (eg for use on head/hand)

### Varaints

Cosmetic variants can be defined by creating subdirectories in the cosmetic directory. Variants should follow
the same file formats as top level cosmetics.

### TODOs
- Support for coloring the 2d icon
