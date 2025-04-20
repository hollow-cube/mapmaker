declare global {
    interface Console {
        log(message?: string, ...optionalParams: unknown[]): void;
        warn(message?: string, ...optionalParams: unknown[]): void;
        error(message?: string, ...optionalParams: unknown[]): void;
    }

    const console: Console;
}

export {};