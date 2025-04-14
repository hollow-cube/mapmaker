package net.hollowcube.datafix;

import net.hollowcube.datafix.versions.v0xxx.*;
import net.hollowcube.datafix.versions.v1xxx.*;
import net.hollowcube.datafix.versions.v2xxx.*;
import net.hollowcube.datafix.versions.v3xxx.*;
import net.hollowcube.datafix.versions.v4xxx.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DataFixes {
    private static List<DataType> dataTypes = new ArrayList<>();

    public static void addFixVersions(@NotNull List<Supplier<DataVersion>> versions) {
        for (var version : versions) version.get();
    }

    public static void build() {

    }

    static void addDataType(@NotNull DataType knownType) {
        dataTypes.add(knownType);
    }

    static {
        addFixVersions(List.of( // 0xxx
                V99::new,
                V100::new,
                V101::new,
                V102::new,
                V105::new,
                V107::new,
                V108::new,
                V109::new,
                V110::new,
                V111::new,
                V113::new,
                V135::new,
                V143::new,
                V147::new,
                V165::new,
                V501::new,
                V502::new,
                V700::new,
                V701::new,
                V702::new,
                V703::new,
                V704::new,
                V705::new,
                V804::new,
                V806::new,
                V808::new,
                V808_1::new,
                V813::new,
                V820::new
        ));
        addFixVersions(List.of( // 1xxx
                V1125::new,
                V1450::new,
                V1451::new,
                V1451_2::new,
                V1451_3::new,
                V1451_5::new,
                V1456::new,
                V1458::new,
                V1460::new,
                V1470::new,
                V1474::new,
                V1475::new,
                V1480::new,
                V1484::new,
                V1486::new,
                V1487::new,
                V1488::new,
                V1490::new,
                V1494::new,
                V1500::new,
                V1510::new,
                V1515::new,
                V1624::new,
                V1800::new,
                V1801::new,
                V1802::new,
                V1803::new,
                V1904::new,
                V1906::new,
                V1909::new,
                V1914::new,
                V1917::new,
                V1918::new,
                V1920::new,
                V1928::new,
                V1929::new,
                V1931::new,
                V1948::new,
                V1953::new,
                V1955::new,
                V1963::new
        ));
        addFixVersions(List.of( // 2xxx
                V2100::new,
                V2209::new,
                V2501::new,
                V2502::new,
                V2503::new,
                V2505::new,
                V2508::new,
                V2509::new,
                V2511::new,
                V2511_1::new,
                V2514::new,
                V2516::new,
                V2518::new,
                V2519::new,
                V2522::new,
                V2523::new,
                V2528::new,
                V2529::new,
                V2531::new,
                V2533::new,
                V2535::new,
                V2552::new,
                V2553::new,
                V2568::new,
                V2571::new,
                V2679::new,
                V2680::new,
                V2684::new,
                V2686::new,
                V2688::new,
                V2690::new,
                V2691::new,
                V2700::new,
                V2702::new,
                V2704::new,
                V2707::new,
                V2717::new,
                V2838::new,
                V2843::new
        ));
        addFixVersions(List.of( // 3xxx
                V3076::new,
                V3078::new,
                V3081::new,
                V3082::new,
                V3083::new,
                V3086::new,
                V3087::new,
                V3090::new,
                V3093::new,
                V3094::new,
                V3097::new,
                V3202::new,
                V3203::new,
                V3204::new,
                V3209::new,
                V3322::new,
                V3325::new,
                V3326::new,
                V3327::new,
                V3328::new,
                V3438::new,
                V3439::new,
                V3447::new,
                V3448::new,
                V3564::new,
                V3568::new,
                V3682::new,
                V3683::new,
                V3685::new,
                V3689::new,
                V3692::new,
                V3799::new,
                V3800::new,
                V3803::new,
                V3807::new,
                V3808::new,
                V3808_1::new,
                V3808_2::new,
                V3809::new,
                V3812::new,
                V3813::new,
                V3814::new,
                V3816::new,
                V3818::new,
                V3818_1::new,
                V3818_3::new,
                V3818_5::new,
                V3825::new,
                V3833::new,
                V3938::new,
                V3945::new
        ));
        addFixVersions(List.of( // 4xxx
                V4054::new,
                V4055::new,
                V4059::new,
                V4064::new,
                V4067::new,
                V4068::new,
                V4070::new,
                V4071::new,
                V4173::new,
                V4175::new,
                V4181::new,
                V4187::new,
                V4290::new,
                V4292::new,
                V4300::new,
                V4301::new,
                V4302::new,
                V4306::new,
                V4307::new
        ));
    }
}
