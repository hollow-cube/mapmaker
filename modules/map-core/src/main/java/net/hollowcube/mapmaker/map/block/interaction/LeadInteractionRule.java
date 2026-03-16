package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.mapmaker.map.entity.impl.other.LeashKnotEntity;

import java.util.UUID;

public class LeadInteractionRule implements BlockInteractionRule {
    public static final LeadInteractionRule INSTANCE = new LeadInteractionRule();

    private LeadInteractionRule() {
    }

    @Override
    public boolean handleInteraction(Interaction interaction) {
        if (BlockTags.FENCES.contains(interaction.getBlock(interaction.blockPosition()).key())) {

            var entity = new LeashKnotEntity(UUID.randomUUID());
            entity.setInstance(interaction.instance(), interaction.blockPosition().add(0.5, 0.5, 0.5));

            return true;
        }
        return false;
    }
}
