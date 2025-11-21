package net.hollowcube.mapmaker.scripting;

/// All thread data types used in lua must implement this interface.
public interface ThreadData {

    ScriptContext scriptContext();
    
}
