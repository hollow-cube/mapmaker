import {useViewStack} from "@mapmaker/gui";

export default function BackButton() {
    const {close} = useViewStack();

    return (
        <button onLeftClick={close}>
            <tooltip translationKey="gui.generic.close_menu">
                <sprite src='generic2/btn/danger/1_1' position='absolute'/>
                {/* TODO: it would be nice if we could use x='center' here. */}
                {/* TODO support for back arrow conditionally*/}
                <sprite src='generic2/btn/common/close' x={4} y={4}
                        slotWidth={1} slotHeight={1}/>
            </tooltip>
        </button>
    )

}