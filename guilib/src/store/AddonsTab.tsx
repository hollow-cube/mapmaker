import {ElementProps} from "@mapmaker/gui";

type Addon = ElementProps<'sprite'> & {
    id: string,
    translationKey: string,
}

interface AddonChainProps {
    chain: (Addon | undefined)[],
}

function isPackageOwned(id: string): boolean {
    return false;
}

// TODO: this needs to be implemented in java
function getPackageCost(id: string): 50 | 100 | 150 {
    return 50;
}

function AddonChain({chain}: AddonChainProps) {
    const firstLocked = chain.find(addon => !isPackageOwned(addon?.id || ''));
    const addon = firstLocked || chain[chain.length - 1];

    if (!addon) {
        return <sprite src={'store/addons/slot_default'} slotWidth={1} slotHeight={1}/>
    }

    return (
        <button>
            <tooltip translationKey={addon.translationKey} slotWidth={1} slotHeight={1}>
                <sprite src={!firstLocked ? 'store/addons/slot_selected' : 'store/addons/slot_default'}/>
                <sprite {...addon} />

                {firstLocked && <sprite {...costs[getPackageCost(firstLocked.id)]} y={24}/>}
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
        src: 'store/addons/map_slot',
        x: 4,
        y: 4,
    },
    {
        id: 'map_slot_4',
        translationKey: 'map_slot_4',
        src: 'store/addons/map_slot',
        x: 4,
        y: 4,
    },
    {
        id: 'map_slot_5',
        translationKey: 'map_slot_5',
        src: 'store/addons/map_slot',
        x: 4,
        y: 4,
    },
] as const;

const mapSizes: Addon[] = [
    {
        id: 'map_size_2',
        translationKey: 'map_size_2',
        src: 'store/addons/map_size_2',
        x: 3,
        y: 4,
    },
    {
        id: 'map_size_3',
        translationKey: 'map_size_3',
        src: 'store/addons/map_size_3',
        x: 3,
        y: 3,
    },
    {
        id: 'map_size_4',
        translationKey: 'map_size_4',
        src: 'store/addons/map_size_4',
        x: 2,
        y: 2,
    },
] as const;

const boosts: Addon[] = [{
    id: 'boosts',
    translationKey: 'boosts',
    src: 'store/addons/map_boost',
    x: 5,
    y: 1,
}] as const;

const buildTools: Addon[] = [{
    id: 'build_tools',
    translationKey: 'build_tools',
    src: 'store/addons/build_tools',
    x: 3,
    y: 2,
}] as const;

export default function AddonsTab() {
    return (
        <group layout='column'>
            <sprite src='store/addons/container' position='absolute' y={1}/>

            <gap slotHeight={1}/>

            <group layout='row'>
                <gap slotWidth={1}/>

                <AddonChain chain={mapSlots}/>

                <gap slotWidth={1}/>

                <AddonChain chain={mapSizes}/>

                <gap slotWidth={1}/>

                <AddonChain chain={boosts}/>

                <gap slotWidth={1}/>

                <AddonChain chain={buildTools}/>
            </group>

            <gap slotHeight={1}/>

            <group layout='row'>
                <gap slotWidth={1}/>

                <AddonChain chain={[]}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[]}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[]}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[]}/>
            </group>

            <gap slotHeight={1}/>

        </group>
    )
}
