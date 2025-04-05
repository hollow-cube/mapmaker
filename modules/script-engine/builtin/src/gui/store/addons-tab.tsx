import {ElementProps, useViewStack} from "@mapmaker/gui";
import {buyUpgrade, isUpgradeOwned} from "@mapmaker/internal/store";
import Confirm from "../lib/confirm";

type Addon = ElementProps<'sprite'> & {
    id: string,
    translationKey: string,
    cost: 50 | 100 | 150,
}

interface AddonChainProps {
    chain: (Addon | undefined)[];
    buyUpgrade: (id: string) => void;
}

function AddonChain({chain, buyUpgrade}: AddonChainProps) {
    const firstLocked = chain.find(addon => !isUpgradeOwned(addon?.id || ''));
    const addon = firstLocked || chain[chain.length - 1];

    if (!addon) {
        return <sprite src={'store/addons/slot_default'} slotWidth={1} slotHeight={1}/>
    }

    return (
        <button onLeftClick={() => {
            if (firstLocked) buyUpgrade(firstLocked.id)
        }}>
            <tooltip translationKey={addon.translationKey} slotWidth={1} slotHeight={1}>
                <sprite src={!firstLocked ? 'store/addons/slot_selected' : 'store/addons/slot_default'}/>
                <sprite {...addon} />

                {firstLocked && <sprite {...costs[firstLocked.cost]} y={24}/>}
            </tooltip>
        </button>
    )
}

const costs = {
    50: {src: 'store/addons/cost_50_cubits', x: 1},
    100: {src: 'store/addons/cost_100_cubits', x: -1},
    150: {src: 'store/addons/cost_150_cubits', x: -1},
} as const;

const mapSlots: Addon[] = [
    {
        id: 'map_slot_3',
        translationKey: 'map_slot_3',
        cost: 50,
        src: 'store/addons/map_slot',
        x: 4,
        y: 4,
    },
    {
        id: 'map_slot_4',
        translationKey: 'map_slot_4',
        cost: 100,
        src: 'store/addons/map_slot',
        x: 4,
        y: 4,
    },
    {
        id: 'map_slot_5',
        translationKey: 'map_slot_5',
        cost: 150,
        src: 'store/addons/map_slot',
        x: 4,
        y: 4,
    },
] as const;

const mapSizes: Addon[] = [
    {
        id: 'map_size_2',
        translationKey: 'map_size_2',
        cost: 50,
        src: 'store/addons/map_size_2',
        x: 3,
        y: 4,
    },
    {
        id: 'map_size_3',
        translationKey: 'map_size_3',
        cost: 100,
        src: 'store/addons/map_size_3',
        x: 3,
        y: 3,
    },
    {
        id: 'map_size_4',
        translationKey: 'map_size_4',
        cost: 150,
        src: 'store/addons/map_size_4',
        x: 2,
        y: 2,
    },
] as const;

const buildTools: Addon[] = [{
    id: 'build_tools',
    translationKey: 'build_tools',
    cost: 50,
    src: 'store/addons/build_tools',
    x: 3,
    y: 2,
}] as const;

export default function AddonsTab() {
    const {pushView} = useViewStack();

    const buyUpgradeActual = (id: string) => {
        pushView(<Confirm onConfirm={() => buyUpgrade(id)}/>)
    }

    return (
        <group layout='column'>
            <sprite src='store/addons/container' position='absolute' y={1}/>

            <gap slotHeight={1}/>

            <group layout='row'>
                <gap slotWidth={1}/>

                <AddonChain chain={mapSlots} buyUpgrade={buyUpgradeActual}/>

                <gap slotWidth={1}/>

                <AddonChain chain={mapSizes} buyUpgrade={buyUpgradeActual}/>

                <gap slotWidth={1}/>

                <AddonChain chain={buildTools} buyUpgrade={buyUpgradeActual}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[]} buyUpgrade={buyUpgradeActual}/>
            </group>

            <gap slotHeight={1}/>

            <group layout='row'>
                <gap slotWidth={1}/>

                <AddonChain chain={[]} buyUpgrade={buyUpgradeActual}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[]} buyUpgrade={buyUpgradeActual}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[]} buyUpgrade={buyUpgradeActual}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[]} buyUpgrade={buyUpgradeActual}/>
            </group>

            <gap slotHeight={1}/>

        </group>
    )
}
