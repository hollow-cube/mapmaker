declare module "@mapmaker" {


    // TEXT COMPONENTS

    declare type Text = { readonly __typeof: unique symbol };
    declare function mm(strings: TemplateStringsArray, ...expr: string[]): Text;


    // MAPS

    declare type MapDifficulty = 'easy' | 'medium' | 'hard' | 'expert' | 'nightmare';
    declare type MapQuality = 'unrated' | 'good' | 'great' | 'excellent' | 'outstanding' | 'masterpiece';

    declare type Map = {
        id: string;
        owner: string;
        name: string;
    } & ({
        isPublished: false,
    } | {
        isPublished: true,
        publishedId: string;

        difficulty: MapDifficulty;
        quality: MapQuality;

        likes: number;
        uniquePlays: number;
    });
    declare type PublishedMap = Extract<Map, { isPublished: true }>;

}

declare module "@mapmaker/gui" {

    // TODO: read https://www.typescriptlang.org/docs/handbook/jsx.html and write better types

    declare type ElementProps<T extends keyof JSX.IntrinsicElements> = {
        [P in keyof JSX.IntrinsicElements[T]]: JSX.IntrinsicElements[T][P];
    }

    declare type InventoryType = 'chest_3_row' | 'chest_6_row' | 'anvil'
    declare function view<T>(component: (props: T) => JSX.Element, inventoryType: InventoryType): (props: T) => JSX.Element;

    declare function useState<T>(initialValue: T | (() => T)): [T, (value: T | ((T) => T)) => void];

    // TODO: usable should be Usable<T> = PromiseLike<T> | Context<T>. Need some more complete react types for what i want to expose.
    export function use<T>(usable: PromiseLike<T>): T;

    declare type ViewStack = {
        pushView: <T>(view: JSX.Element, config?: {
            transient?: boolean,
            replace?: boolean
        }) => void;
        canPopView: boolean;
        popView: () => void;
        close: () => void;
    };
    declare function useViewStack(): ViewStack;

}

declare module "@mapmaker/internal/store" {
    declare type Package =
        'cubits_50'
        | 'cubits_105'
        | 'cubits_220'
        | 'cubits_400'
        | 'cubits_600'
        | 'hypercube_1mo'
        | 'hypercube_1y'

    declare function buyPackage(packageName: Package): Promise<void>

    declare function isUpgradeOwned(upgradeId: string): boolean

    declare function buyUpgrade(upgradeId: string): Promise<void>
}

declare module "@mapmaker/internal/maps" {
    import {MapDifficulty, MapQuality, PublishedMap} from "@mapmaker";

    declare type Sort = 'best' | 'quality' | 'new';
    declare type Order = 'asc' | 'desc';

    declare type SearchMapsParams = {
        page?: number;
        pageSize?: number;

        sort?: Sort;
        sortOrder?: Order;

        difficulty?: MapDifficulty[];
        quality?: MapQuality[];

        parkour?: boolean;
        building?: boolean;
    }

    declare type SearchMapsResponse = {
        page: number;
        pageCount?: number; // Only present on first page.
        results: PublishedMap[];
    }

    declare function searchMaps(params: SearchMapsParams): Promise<SearchMapsResponse>;

}
