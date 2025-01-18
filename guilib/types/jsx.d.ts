declare namespace JSX {
    export type Element = any;

    export interface IntrinsicElements {
        row: { [key: string]: any };
        column: { [key: string]: any };

        sprite: {
            layer?: 'container' | 'background'; // Default container
            src: string;
            x?: number;
            y?: number;
        }

        text: { [key: string]: any };
        button: {
            translationKey?: string;
            onclick?: () => void;
            width?: number;
            height?: number;
        };

        div: { [key: string]: any };
        span?: { style?: string };
    }

    export const Fragment: any;

    export function createElement(tag: string, props: any, ...children: any[]): any;
}