import {useState, view} from "@mapmaker/gui";

import BackButton from "../lib/back-button";
import CubitsTab from "./cubits-tab";
import HypercubeTab from "./hypercube-tab";
import AddonsTab from "./addons-tab";

const tabTitles = {
    cubits: 'Buy Cubits',
    hypercube: 'Buy Hypercube',
    addons: 'Buy Addons'
}

function StoreView() {
    const [tab, setTab] = useState<'cubits' | 'hypercube' | 'addons'>('cubits');

    return (
        <group layout='column'>
            <sprite src='store/container' x={-10} y={-31} position='absolute'/>
            <text x='center' y={-23} position='absolute'>Store</text>

            {/* Title */}
            <group layout='row'>
                <BackButton/>

                <tooltip translationKey="gui.store.information" slotWidth={1} slotHeight={1}>
                    <sprite src='generic2/btn/default/1_1' position='absolute'/>
                    <sprite src="generic2/btn/common/info" x={4} y={2}/>
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
                        <sprite src={tab === 'cubits' ? 'store/cubits_active' : 'store/cubits_inactive'}/>
                    </tooltip>
                </button>
                <button onLeftClick={() => setTab('hypercube')}>
                    <tooltip translationKey='gui.store.hypercube' slotWidth={3} slotHeight={3}>
                        <sprite src={tab === 'hypercube' ? 'store/ranks_active' : 'store/ranks_inactive'}/>
                    </tooltip>
                </button>
                <button onLeftClick={() => setTab('addons')}>
                    <tooltip translationKey='gui.store.addons' slotWidth={3} slotHeight={3}>
                        <sprite src={tab === 'addons' ? 'store/addons_active' : 'store/addons_inactive'}/>
                    </tooltip>
                </button>
            </group>
        </group>
    )
}

export default view(StoreView, 'chest_6_row');