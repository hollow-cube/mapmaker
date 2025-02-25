declare module "@mapmaker" {

}

declare module "@mapmaker/gui" {
    declare function useState<T>(initialValue: T | (() => T)): [T, (value: T) => void];
}
