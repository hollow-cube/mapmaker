package net.hollowcube.mapmaker.scripting;

/// All thread data types used in lua must implement this interface.
public interface ThreadData {

    ScriptContext scriptContext();

    // TODO: need to assign the thread data callback to set user data = parent script context so we know there is always one.
//    final class Task implements ThreadData {
//        private final ScriptContext scriptContext;
//
//        private net.minestom.server.timer.Task task = null;
//
//        public Task(ScriptContext scriptContext) {
//            this.scriptContext = scriptContext;
//        }
//
//
//    }

}
