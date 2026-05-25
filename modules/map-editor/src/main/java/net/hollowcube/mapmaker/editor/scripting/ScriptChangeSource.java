package net.hollowcube.mapmaker.editor.scripting;

import java.io.Closeable;

/// Something that observes script file changes for one editing world and feeds
/// them into a [ReloadingScriptSession]. Implementations only *notify* - the
/// blocking re-fetch and the thread-confined, debounced reload are owned by
/// [ReloadingScriptSession#notifyFilesChanged].
///
/// There is one change source per editing world; it is started when the world
/// opens and [#close]d when it closes.
///
/// The only implementation that matters right now is [NatsChangeSource]; a
/// filesystem-backed one (local dev) can implement this same interface later.
public interface ScriptChangeSource extends Closeable {

    /// Begin observing. Should be called before [ReloadingScriptSession#bootstrap]
    /// so changes that land during the initial fetch are not missed.
    void start();

    @Override
    void close();
}
