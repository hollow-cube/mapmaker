import {buyPackage} from "@mapmaker/internal/store";

export default function CubitsTab() {
    return (
        <group layout='column'>
            <sprite src='store/cubits' position='absolute' y={1}/>

            <group layout='row'>
                <button onLeftClick={() => buyPackage('cubits_50')}>
                    <tooltip translationKey="gui.store.cubits.1" slotWidth={3} slotHeight={2}/>
                </button>
                <button onLeftClick={() => buyPackage('cubits_105')}>
                    <tooltip translationKey="gui.store.cubits.2" slotWidth={3} slotHeight={2}/>
                </button>
                <button onLeftClick={() => buyPackage('cubits_220')}>
                    <tooltip translationKey="gui.store.cubits.3" slotWidth={3} slotHeight={2}/>
                </button>
            </group>

            <group layout='row'>
                <button onLeftClick={() => buyPackage('cubits_400')}>
                    <tooltip translationKey="gui.store.cubits.4" slotWidth={4} slotHeight={3}/>
                </button>

                <gap slotWidth={1}/>

                <button onLeftClick={() => buyPackage('cubits_600')}>
                    <tooltip translationKey="gui.store.cubits.5" slotWidth={4} slotHeight={3}/>
                </button>
            </group>
        </group>
    )
}
