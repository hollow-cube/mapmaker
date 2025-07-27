package net.hollowcube.mapmaker.config;

import net.hollowcube.common.util.RuntimeGson;

// Yes i know the underscore is gross, but i dont want to deal with changing the parser
@RuntimeGson
public record Map_ServiceConfig(
        String url
) {
}
