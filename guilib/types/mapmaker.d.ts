declare module "@mapmaker" {


    // TEXT COMPONENTS

    declare type Text = { readonly __typeof: unique symbol };
    declare function mm(strings: TemplateStringsArray, ...expr: string[]): Text;

}

declare module "@mapmaker/gui" {
    declare function useState<T>(initialValue: T | (() => T)): [T, (value: T | ((T) => T)) => void];
}
