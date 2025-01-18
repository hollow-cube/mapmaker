import BackButton from '../../common/BackButton';
import {AddonsTab, CubitsTab, HypercubeTab} from './tab';

const TAB_NAMES = ['Buy Cubits', 'Buy Hypercube', 'Buy Add-Ons'] as const;
const [CUBITS, HYPERCUBE, ADDONS] = [0, 1, 2] as const;

function StoreView() {
    const [tab, setTab] = useShared<0 | 1 | 2>('tab', CUBITS);

    return <column>
        <sprite src='store/container' layer='background' x={-12} y={-49}/>
        <text layer='background'>Store</text>

        <row>
            <BackButton/>
            <div>Info Icon TODO</div>
            <text width={5}>{TAB_NAMES[tab]}</text>
        </row>

        <row>
            {tab === CUBITS && <CubitsTab/>}
            {tab === HYPERCUBE && <HypercubeTab/>}
            {tab === ADDONS && <AddonsTab/>}
        </row>

        <row>
            <button translationKey='cubits' onclick={() => setTab(CUBITS)} width={3} height={3}/>
            <button translationKey='hypercube' onclick={() => setTab(HYPERCUBE)} width={3} height={3}/>
            <button translationKey='addons' onclick={() => setTab(ADDONS)} width={3} height={3}/>
        </row>
    </column>;
}

export default StoreView;
