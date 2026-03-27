package net.hollowcube.mapmaker.util.telemetry;

import com.sun.management.GarbageCollectionNotificationInfo;
import io.prometheus.client.Histogram;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class CustomJVMPrometheus {

    private static final Histogram gcPauseHistogram = Histogram.build()
        .name("jvm_gc_pause_seconds")
        .help("Individual GC pause durations")
        .labelNames("gc", "algorithm")
        .buckets(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10)
        .register();

    public static void init() {
        String algorithm = detectGcAlgorithm();

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gcBean instanceof NotificationEmitter emitter) {
                NotificationListener listener = (notification, _) -> {
                    if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(
                            (CompositeData) notification.getUserData()
                        );

                        double durationSeconds = info.getGcInfo().getDuration() / 1000.0;
                        gcPauseHistogram
                            .labels(info.getGcName(), algorithm)
                            .observe(durationSeconds);
                    }
                };

                emitter.addNotificationListener(listener, null, null);
            }
        }
    }

    private static String detectGcAlgorithm() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName();

            // map them to nice names coz some JVMs do weird things.
            if (name.contains("G1")) {
                return "G1";
            } else if (name.contains("ZGC")) {
                return "ZGC";
            } else if (name.contains("Shenandoah")) {
                return "Shenandoah";
            } else if (name.startsWith("PS")) {
                return "Parallel";
            } else if (name.equals("Copy") || name.equals("MarkSweepCompact")) {
                return "Serial";
            }
        }

        // IDK man what weird ass GC are we using (maybe some fancy new stuff hehe)
        return gcBeans.stream()
            .map(GarbageCollectorMXBean::getName)
            .findFirst()
            .orElse("Unknown");
    }
}
