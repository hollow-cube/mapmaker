package net.hollowcube.datafix.blockEntity;

import net.hollowcube.datafix.AbstractTypedUpgradeTest;
import net.hollowcube.datafix.DataTypes;

abstract class AbstractBlockEntityUpgradeTest extends AbstractTypedUpgradeTest {

    public AbstractBlockEntityUpgradeTest() {
        super(DataTypes.BLOCK_ENTITY, "blockEntity");
    }

}
