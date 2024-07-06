package net.hollowcube.schem.util;

final class NoopGameDataProvider implements GameDataProvider {
    static GameDataProvider INSTANCE = new NoopGameDataProvider();

    @Override
    public int dataVersion() {
        return 3700; // 1.20.4
    }
}
