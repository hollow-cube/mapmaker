package net.hollowcube.mapmaker.permission.client;

import com.authzed.api.v1.PermissionsServiceGrpc;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceBlockingStub;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannelBuilder;
import net.hollowcube.common.config.SpiceDBConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;

public interface SpiceDBClientFactory {

    @NotNull PermissionsServiceBlockingStub newPermissionClient(@NotNull SpiceDBConfig config);

    static @NotNull SpiceDBClientFactory get() {
        class Holder {
            //            static final System.Logger logger = System.getLogger(SpiceDBClientFactory.class.getName());
            static SpiceDBClientFactory instance = null;
        }
        if (Holder.instance == null) {
            Holder.instance = ServiceLoader.load(SpiceDBClientFactory.class).findFirst()
                    .orElseGet(() -> config -> {
                        //todo should reuse permission service if the config is the same
                        var channelBuilder = ManagedChannelBuilder
                                .forTarget(config.address());
                        if (config.tls())
                            channelBuilder.useTransportSecurity();
                        else channelBuilder.usePlaintext();

                        //todo should ping permission service to fail early
                        return PermissionsServiceGrpc.newBlockingStub(channelBuilder.build())
                                .withCallCredentials(new BearerToken(config.secretKey()));
                    });
        }
        return Holder.instance;
    }
}
