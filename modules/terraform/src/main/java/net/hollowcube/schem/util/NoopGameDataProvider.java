package net.hollowcube.schem.util;

final class NoopGameDataProvider implements net.hollowcube.schem.util.GameDataProvider {
    static net.hollowcube.schem.util.GameDataProvider INSTANCE = new NoopGameDataProvider();

    @Override
    public int dataVersion() {
        return 3700; // 1.20.4
    }
}
