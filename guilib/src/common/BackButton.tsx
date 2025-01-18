export default function BackButton() {
    const {canPop, pop, close} = useViewStack();

    return <button
        translationKey={canPop ? 'gui.generic.back_arrow' : 'gui.generic.close_menu'}
        onclick={() => canPop ? pop() : close()}
    >
        <sprite src={canPop ? 'generic/back_bg2' : 'generic/close_bg2'}/>
    </button>
}