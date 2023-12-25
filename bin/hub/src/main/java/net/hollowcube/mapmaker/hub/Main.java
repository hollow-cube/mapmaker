package net.hollowcube.mapmaker.hub;

import net.hollowcube.mapmaker.misc.CoreInit;
import org.slf4j.LoggerFactory;

public class Main {

    public static void main(String[] args) {
        CoreInit.genericStandaloneInit(
                LoggerFactory.getLogger(Main.class),
                HubServerImpl::new
        );
    }

}
