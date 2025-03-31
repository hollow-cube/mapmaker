export default function HypercubeTab() {
    return (
        <group layout='column'>
            <sprite src='store/hypercube' position='absolute' y={1}/>

            {/* The layout here is kinda confusing but basically we have one row with a bunch of vertical
                columns representing the perk displays themselves. */}
            <group layout='row'>
                <tooltip translationKey="gui.store.hypercube.perks.unlimited_map_slots" slotWidth={2} slotHeight={2}/>

                <group layout='column'>
                    <gap slotHeight={1}/>
                    <tooltip translationKey="gui.store.hypercube.perks.beta_testing" slotWidth={2} slotHeight={2}/>
                </group>

                <group layout='column'>
                    <gap slotHeight={1}/>
                    <tooltip translationKey="gui.store.hypercube.perks.badge" slotWidth={1} slotHeight={2}/>
                </group>

                <group layout='column'>
                    <gap slotHeight={1}/>
                    <tooltip translationKey="gui.store.hypercube.perks.more_emojis" slotWidth={2} slotHeight={2}/>
                </group>

                <tooltip translationKey="gui.store.hypercube.perks.all_map_sizes" slotWidth={2} slotHeight={2}/>
            </group>

            <group layout='row'>
                <gap slotWidth={1}/>
                <button>
                    <tooltip translationKey="gui.store.hypercube.1month" slotWidth={3} slotHeight={2}/>
                </button>
                <gap slotWidth={1}/>
                <button>
                    <tooltip translationKey="gui.store.hypercube.1year" slotWidth={3} slotHeight={2}/>
                </button>
            </group>

        </group>
    )
}
