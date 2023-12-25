package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.misc.CoreInit;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        CoreInit.genericStandaloneInit(
                LoggerFactory.getLogger(Main.class),
                MapServerImpl::new
        );
    }
}
