declare namespace JSX {
    export type Element = any;

    export type Layer = 'background' | 'default' | 'item'

    export type Props = { [key: string]: any }

    export interface IntrinsicElements {
        group: Props;

        sprite: Props

        text: Props;

        button: Props;

        tooltip: Props;

        item: Props;

        gap: Props;
    }

    export const Fragment: any;

    export function createElement(tag: string, props: any, ...children: any[]): any;
}