declare global {
    function remoteImpl<C extends Function>(c: C, javaClass: string): C;

    function useState<T>(initialValue: T): [T, (newValue: T) => void];

    function useShared<T>(id: string, initialValue: T): [T, (newValue: T) => void];

    function useViewStack(): {
        canPop: boolean;
        pop: () => void;
        close: () => void;
    }
}

export {};