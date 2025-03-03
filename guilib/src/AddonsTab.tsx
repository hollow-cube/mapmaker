import {ElementProps} from "@mapmaker/gui";

type Addon = ElementProps<'sprite'> & {
    id: string,
    translationKey: string,
}

interface AddonChainProps {
    chain: [Addon, ...Addon[]],
}

function isPackageOwned(id: string): boolean {
    return false;
}

function AddonChain({chain}: AddonChainProps) {
    const firstLocked = chain.find(addon => !isPackageOwned(addon.id));
    const addon = firstLocked || chain[chain.length - 1];

    return (
        <button>
            <tooltip translationKey={addon.translationKey} slotWidth={1} slotHeight={1}>
                <sprite src={!firstLocked ? 'store/addons2/slot_selected' : 'store/addons2/slot_default'}/>
                <sprite {...addon} />
            </tooltip>
        </button>
    )
}

const mapSlots: AddonChainProps['chain'] = [
    {
        id: 'map_slot_2',
        translationKey: 'map_slot_2',
        src: 'store/addons2/map_slot_2',
        x: 3,
        y: 4,
    },
    {
        id: 'map_slot_3',
        translationKey: 'map_slot_3',
        src: 'store/addons2/map_slot_3',
        x: 3,
        y: 4,
    },
    {
        id: 'map_slot_4',
        translationKey: 'map_slot_4',
        src: 'store/addons2/map_slot_4',
        x: 3,
        y: 4,
    },
] as const;

const mapSizes: AddonChainProps['chain'] = [
    {
        id: 'map_size_2',
        translationKey: 'map_size_2',
        src: 'store/addons2/map_size_2',
        x: 3,
        y: 4,
    },
    {
        id: 'map_size_3',
        translationKey: 'map_size_3',
        src: 'store/addons2/map_size_3',
        x: 3,
        y: 4,
    },
    {
        id: 'map_size_4',
        translationKey: 'map_size_4',
        src: 'store/addons2/map_size_4',
        x: 3,
        y: 4,
    },
] as const;

const boosts: Addon = {
    id: 'boosts',
    translationKey: 'boosts',
    src: 'store/addons2/boosts',
    x: 3,
    y: 4,
}

const example: Addon = {
    id: 'map_size_2',
    translationKey: 'map_size_2',
    src: 'store/addons2/map_size_2',
    x: 3,
    y: 4,
}

export default function AddonsTab() {
    return (
        <group layout='column'>
            <sprite src='store/addons2/container' position='absolute' y={1}/>

            <gap slotHeight={1}/>

            <group layout='row'>
                <gap slotWidth={1}/>

                <AddonChain chain={mapSlots}/>

                <gap slotWidth={1}/>

                <AddonChain chain={mapSizes}/>

                <gap slotWidth={1}/>

                <AddonChain chain={boosts}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[example]}/>
            </group>

            <gap slotHeight={1}/>

            <group layout='row'>
                <gap slotWidth={1}/>

                <AddonChain chain={[example]}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[example]}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[example]}/>

                <gap slotWidth={1}/>

                <AddonChain chain={[example]}/>
            </group>

            <gap slotHeight={1}/>

        </group>
    )
}
