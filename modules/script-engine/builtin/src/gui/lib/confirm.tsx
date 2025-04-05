import {useViewStack, view} from "@mapmaker/gui";

export interface ConfirmProps {
    translationKey?: string;
    onConfirm: () => void;
}

function Confirm({translationKey, onConfirm}: ConfirmProps) {
    const {popView} = useViewStack();
    return (
        <group layout='column'>
            <sprite src='generic/confirm_container' position='absolute'/>

            <gap slotHeight={1}/>
            <group layout='row'>
                <gap slotWidth={1}/>
                <button onLeftClick={popView}>
                    <tooltip translationKey='gui.confirm.no' slotWidth={3} slotHeight={1}/>
                </button>
                <gap slotWidth={1}/>
                <button onLeftClick={onConfirm}>
                    <tooltip translationKey={translationKey || 'gui.confirm.yes'} slotWidth={3} slotHeight={1}/>
                </button>
            </group>
        </group>
    )
}

export default view(Confirm, 'chest_3_row');