declare module "@mapmaker" {


    // TEXT COMPONENTS

    declare type Text = { readonly __typeof: unique symbol };
    declare function mm(strings: TemplateStringsArray, ...expr: string[]): Text;

}

declare module "@mapmaker/gui" {

    declare type InventoryType = 'chest_6_row' | 'anvil'
    declare type ViewElement = JSX.Element & { readonly __typeof: unique symbol };
    declare type ViewType<T extends (props: T) => JSX.Element> = ((props: Parameters<T>[0]) => ViewElement) & {
        readonly __typeof: unique symbol
    };
    declare function view<T>(component: (props: T) => JSX.Element, inventoryType: InventoryType): ViewType<T>;

    declare type ElementProps<T extends keyof JSX.IntrinsicElements> = {
        [P in keyof JSX.IntrinsicElements[T]]: JSX.IntrinsicElements[T][P];
    }

    declare function useState<T>(initialValue: T | (() => T)): [T, (value: T | ((T) => T)) => void];

    declare type ViewStack = {
        pushView: <T>(view: ViewElement, config?: {
            transient?: boolean,
            replace?: boolean
        }) => void;
        popView: () => void;
        canPopView: () => boolean;
        close: () => void;
    };
    declare function useViewStack(): ViewStack;

}
