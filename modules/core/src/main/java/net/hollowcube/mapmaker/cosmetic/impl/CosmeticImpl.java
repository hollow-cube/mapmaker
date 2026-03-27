package net.hollowcube.mapmaker.cosmetic.impl;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/*

WHAT DO THE COSMETICS NEED TO BE ABLE TO DO FUNCTIONALITY WISE

- hats
  - add to icon slot
  - remove from icon slot
- backpacks
  - add/remove to icon slot
  - add/remove zombie entity
- accessories
  - icon slot (off hand)
- pets
  - icon, spawn entity
- emotes
  - icon, play animation on trigger (separate system)
- particles
  - icon, begin/end particles
- victory effects
  - icon, play animation on trigger (can cast to victory effect applicator)




 */


public class CosmeticImpl {
    protected final Cosmetic cosmetic;

    public CosmeticImpl(@NotNull Cosmetic cosmetic) {
        this.cosmetic = cosmetic;
    }

    public @NotNull ItemStack iconItem() {
        return cosmetic.iconItem();
    }

    public void apply(@NotNull Player player) {
    }

}
