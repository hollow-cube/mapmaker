import {use, useState, view} from "@mapmaker/gui";
import type {SearchMapsResponse} from "@mapmaker/internal/maps";
import BackButton from "../lib/back-button";

interface SearchResultsProps {
    resultPromise: Promise<SearchMapsResponse>;
}

function SearchResults({resultPromise}: SearchResultsProps) {
    const response = use(resultPromise);

    return (
        <group layout='column'>

            {/* TODO: need to support wrapping. */}
            <group layout='row' slotWidth={7} slotHeight={4} wrap>
                {/* The first row is unused in the simple layout */}
                {/*<gap slotWidth={7} slotHeight={1}/>*/}

                {response.results.map(result => (
                    <item key={result.id} model='minecraft:diamond'/>
                ))}
            </group>

            <group layout='row'>
                <gap slotWidth={1} slotHeight={1}/>

                <tooltip translationKey='back' slotWidth={1} slotHeight={1}/>

                <text slotWidth={3} slotHeight={1} x='center' y='center'>
                    {response.page} / {response.pageCount}
                </text>
            </group>
        </group>
    )
}

function MapBrowser() {
    const [sort, setSort] = useState<'best' | 'quality' | 'new'>('best');

    const res = Promise.resolve({
        page: 1,
        pageCount: 10,
        results: Array.from({length: 6}, (_, i) => ({id: `${i}`})),
    } as unknown as SearchMapsResponse)

    return (
        <group layout='column'>
            <sprite src='map_browser/container' x={-10} y={-31} position='absolute'/>
            <text x='center' y={-23} position='absolute'>Play Maps</text>

            {/* Header */}
            <group layout='row'>
                <BackButton/>

                <button>
                    <tooltip translationKey='search.todo' slotWidth={8} slotHeight={1}>
                        {/* This texture is part of container, not sure it should be. */}
                    </tooltip>
                </button>
            </group>

            {/* Items todo */}
            {/* TODO: If a suspense boundary is not hit, we get a REALLY bad error back. We need to somehow handle
                 this case and give a useful error... */}
            <group layout='row'>
                <gap slotWidth={1} slotHeight={5}/>

                <JSX.Suspense fallback={<text>HEllo fallback</text>}>
                    <SearchResults resultPromise={res}/>
                </JSX.Suspense>
            </group>

            {/* Options */}
            <group layout='row'>
                <button onLeftClick={() => setSort('quality')}>
                    <tooltip translationKey='quality' slotWidth={3} slotHeight={1}>
                        {/* TODO: why does this need -1? */}
                        <sprite
                            src={sort === 'quality' ? 'map_browser/tabs/quality_selected' : 'map_browser/tabs/quality_unselected'}
                            y={-1}/>
                    </tooltip>
                </button>
                <button onLeftClick={() => setSort('best')}>
                    <tooltip translationKey='best' slotWidth={3} slotHeight={1}>
                        {/* TODO: why does this need -1? */}
                        <sprite
                            src={sort === 'best' ? 'map_browser/tabs/best_selected' : 'map_browser/tabs/best_unselected'}
                            y={-1}/>
                    </tooltip>
                </button>
                <button onLeftClick={() => setSort('new')}>
                    <tooltip translationKey='new' slotWidth={3} slotHeight={1}>
                        {/* TODO: why does this need -1? */}
                        <sprite
                            src={sort === 'new' ? 'map_browser/tabs/new_selected' : 'map_browser/tabs/new_unselected'}
                            y={-1}/>
                    </tooltip>
                </button>
            </group>

            <gap slotWidth={9} slotHeight={1}/>

            <group layout='row'>
                <gap slotWidth={2}/>

            </group>

        </group>
    )
}

export default view(MapBrowser, 'chest_6_row')