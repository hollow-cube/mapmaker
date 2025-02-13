declare namespace JSX {
    export type Element = any;

    export type Layer = 'background' | 'default' | 'item'

    export type BaseProps = {
        layer?: Layer;
        x?: number;
        y?: number;
    }

    export interface IntrinsicElements {
        row: { [key: string]: any };
        column: { [key: string]: any };

        sprite: BaseProps & {
            src: string;
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