export const inventoryType = "chest_6_row";

function CubitsTab() {
    return (
        <group layout='column'>
            <sprite src='store/cubits' position='absolute' y={1}/>

            <group layout='row'>

                <button onLeftClick={() => console.log('cubits1')}>
                    <tooltip translationKey="gui.store.cubits.1" slotWidth={3} slotHeight={2}/>
                </button>
                <button onLeftClick={() => console.log('cubits2')}>
                    <tooltip translationKey="gui.store.cubits.2" slotWidth={3} slotHeight={2}/>
                </button>
                <button onLeftClick={() => console.log('cubits3')}>
                    <tooltip translationKey="gui.store.cubits.3" slotWidth={3} slotHeight={2}/>
                </button>
            </group>

            <group layout='row'>
                <tooltip translationKey="gui.store.cubits.4" slotWidth={4} slotHeight={3}/>

                {/* TODO: spacer element? */}
                <text slotWidth={1}/>

                <tooltip translationKey="gui.store.cubits.5" slotWidth={4} slotHeight={3}/>
            </group>
        </group>
    )
}

export default function StoreView() {
    return (
        <group layout='column'>
            <sprite src='store/container' x={-10} y={-31} position='absolute'/>
            <text x='center' y={-23} position='absolute'>Store</text>

            {/* Title */}
            <group layout='row'>
                <button onLeftClick={() => console.log('back')}>
                    <tooltip translationKey="generic.back">
                        <sprite src='generic2/btn/danger/1_1' position='absolute'/>
                        {/*<sprite src='generic/back' padding={1}/>*/}

                        <item id='minecraft:stone'/>
                    </tooltip>
                </button>

                <tooltip translationKey="gui.store.information" slotWidth={1} slotHeight={1}>
                    <sprite src='generic2/btn/default/1_1' position='absolute'/>
                    {/*<sprite src="generic/information"/>*/}
                </tooltip>

                <tooltip translationKey="gui.store.buy_cubits" slotWidth={5} slotHeight={1}>
                    <sprite src='generic2/btn/default/5_1'/>
                    <text x='center' y='center' position='absolute'>Buy Cubits</text>
                </tooltip>

                <tooltip translationKey="gui.store.cubits_to_coins" slotWidth={2} slotHeight={1}>
                    <sprite src='generic2/btn/default/2_1'/>
                    <sprite src='store/coins_to_cubits' x={2} y={3}/>
                </tooltip>
            </group>

            {/* TODO: the below group could be in its own BuyCubits component */}
            <CubitsTab/>

            {/* Tab Switcher */}
            
        </group>
    )
}