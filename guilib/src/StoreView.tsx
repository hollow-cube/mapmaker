import {useState} from "@mapmaker/gui";
import CubitsTab from "./CubitsTab";

export const inventoryType = "chest_6_row";

export default function StoreView() {
    const [i, setI] = useState(0);

    return (
        <group layout='column'>
            <sprite src='store/container' x={-10} y={-31} position='absolute'/>
            <text x='center' y={-23} position='absolute'>Store {i}</text>

            {/* Title */}
            <group layout='row'>
                <button onLeftClick={() => {
                    console.log('---- RIGHT BEFORE SETTING STATE ----');
                    setI(current => current + 1);
                }}>
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
            {i === 1 && <CubitsTab/>}

            {/* Tab Switcher */}

        </group>
    )
}