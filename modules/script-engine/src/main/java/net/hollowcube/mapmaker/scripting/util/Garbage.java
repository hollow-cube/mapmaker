package net.hollowcube.mapmaker.scripting.util;

public class Garbage {

    // language=JS
    public static final String REACT_REFRESH_MODULE_TEMPLATE = """
            var prevRefreshReg = window.$RefreshReg$;
            var prevRefreshSig = window.$RefreshSig$;
            var RefreshRuntime = require('react-refresh/runtime');
            
            window.$RefreshReg$ = (type, id) => {
              const fullId = __hollowcube_moduleId + ' ' + id;
              RefreshRuntime.register(type, fullId);
            }
            window.$RefreshSig$ = RefreshRuntime.createSignatureFunctionForTransform;
            
            try {
            
                %s
            
            } finally {
              window.$RefreshReg$ = prevRefreshReg;
              window.$RefreshSig$ = prevRefreshSig;
            }
            
            enqueueUpdate()
            """;
}
