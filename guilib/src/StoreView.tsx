import {useState, useViewStack, view} from "@mapmaker/gui";

import CubitsTab from "./CubitsTab";
import HypercubeTab from "./HypercubeTab";
import AddonsTab from "./AddonsTab";

const tabTitles = {
    cubits: 'Buy Cubits',
    hypercube: 'Buy Hypercube',
    addons: 'Buy Addons'
}

function StoreView() {
    const {pushView, close} = useViewStack();
    const [tab, setTab] = useState<'cubits' | 'hypercube' | 'addons'>('cubits');

    const Addons2 = view(AddonsTab, 'anvil');
    pushView(<Addons2 />);

    return (
        <group layout='column'>
            <sprite src='store/container' x={-10} y={-31} position='absolute'/>
            <text x='center' y={-23} position='absolute'>Store</text>

            {/* Title */}
            <group layout='row'>
                <button onLeftClick={close}>
                    <tooltip translationKey="generic.back">
                        <sprite src='generic2/btn/danger/1_1' position='absolute'/>
                        {/*<sprite src='generic/back' padding={1}/>*/}

                        <item model='minecraft:stone'/>
                    </tooltip>
                </button>

                <tooltip translationKey="gui.store.information" slotWidth={1} slotHeight={1}>
                    <sprite src='generic2/btn/default/1_1' position='absolute'/>
                    {/*<sprite src="generic/information"/>*/}
                </tooltip>

                <tooltip translationKey="gui.store.buy_cubits" slotWidth={5} slotHeight={1}>
                    <sprite src='generic2/btn/default/5_1'/>
                    <text x='center' y='center' position='absolute'>{tabTitles[tab]}</text>
                </tooltip>

                <tooltip translationKey="gui.store.cubits_to_coins" slotWidth={2} slotHeight={1}>
                    <sprite src='generic2/btn/default/2_1'/>
                    <sprite src='store/coins_to_cubits' x={2} y={3}/>
                </tooltip>
            </group>

            {tab === 'cubits' && <CubitsTab/>}
            {tab === 'hypercube' && <HypercubeTab/>}
            {tab === 'addons' && <AddonsTab/>}

            {/* Tab Switcher */}
            <group layout='row'>
                <button onLeftClick={() => setTab('cubits')}>
                    <tooltip translationKey='gui.store.cubits' slotWidth={3} slotHeight={3}>
                        <sprite src={tab === 'cubits' ? 'store/cubits_inactive' : 'store/cubits_active'}/>
                    </tooltip>
                </button>
                <button onLeftClick={() => setTab('hypercube')}>
                    <tooltip translationKey='gui.store.hypercube' slotWidth={3} slotHeight={3}>
                        <sprite src={tab === 'hypercube' ? 'store/ranks_inactive' : 'store/ranks_active'}/>
                    </tooltip>
                </button>
                <button onLeftClick={() => setTab('addons')}>
                    <tooltip translationKey='gui.store.addons' slotWidth={3} slotHeight={3}>
                        <sprite src={tab === 'addons' ? 'store/add-ons_inactive' : 'store/add-ons_active'}/>
                    </tooltip>
                </button>
            </group>

        </group>
    )
}

export default view(StoreView, 'chest_6_row');