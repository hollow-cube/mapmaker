export default function CubitsTab() {
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
