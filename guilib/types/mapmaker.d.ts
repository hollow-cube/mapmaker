declare module "@mapmaker" {


    // TEXT COMPONENTS

    declare type Text = { readonly __typeof: unique symbol };
    declare function mm(strings: TemplateStringsArray, ...expr: string[]): Text;

}

declare module "@mapmaker/gui" {

    // TODO: read https://www.typescriptlang.org/docs/handbook/jsx.html and write better types

    declare type ElementProps<T extends keyof JSX.IntrinsicElements> = {
        [P in keyof JSX.IntrinsicElements[T]]: JSX.IntrinsicElements[T][P];
    }

    declare type InventoryType = 'chest_6_row' | 'anvil'
    declare function view<T>(component: (props: T) => JSX.Element, inventoryType: InventoryType): JSX.Element;

    declare function useState<T>(initialValue: T | (() => T)): [T, (value: T | ((T) => T)) => void];

    declare type ViewStack = {
        pushView: <T>(view: JSX.Element, config?: {
            transient?: boolean,
            replace?: boolean
        }) => void;
        popView: () => void;
        canPopView: () => boolean;
        close: () => void;
    };
    declare function useViewStack(): ViewStack;

}
