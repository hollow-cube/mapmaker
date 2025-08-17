package net.hollowcube.mapmaker.map.util;

public interface PlayerRiptideExtension {

    void beginRiptideAttack(int durationTicks);

    /**
     * Noop if there is no active riptide attack.
     */
    void cancelRiptideAttack();

}
