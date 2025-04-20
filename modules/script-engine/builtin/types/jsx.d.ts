declare namespace JSX {
    // TODO: read this: https://www.typescriptlang.org/docs/handbook/jsx.html

    export type Element = unknown;

    export type Layer = 'background' | 'default' | 'item'

    type Props = { [key: string]: any };
    type BaseProps = {
        key?: string | null; // React key

        slotWidth?: number;
        slotHeight?: number;

        position?: 'absolute';
    }
    type GroupProps = BaseProps & {
        layout?: 'row' | 'column';
        wrap?: boolean;
    }
    type Alignment = number | 'center' | 'start' | 'end';
    type PositionProps = BaseProps & {
        x?: number | 'center' | 'start' | 'end';
        y?: number | 'center' | 'start' | 'end';
    }

    export interface IntrinsicElements {
        group: GroupProps;
        button: GroupProps & {
            onLeftClick?: () => (void | Promise<void>);
        };
        tooltip: GroupProps & {
            translationKey: string;
        };

        text: PositionProps;
        sprite: PositionProps & {
            src: string;
        };
        item: BaseProps & {
            model: string;
        };

        gap: BaseProps;
    }

    export const Fragment: any;
    export const Suspense: any;

    export function createElement(tag: string, props: any, ...children: any[]): any;
}