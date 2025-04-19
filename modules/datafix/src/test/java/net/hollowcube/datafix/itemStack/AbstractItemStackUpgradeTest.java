package net.hollowcube.datafix.itemStack;

import net.hollowcube.datafix.AbstractTypedUpgradeTest;
import net.hollowcube.datafix.DataTypes;

abstract class AbstractItemStackUpgradeTest extends AbstractTypedUpgradeTest {

    public AbstractItemStackUpgradeTest() {
        super(DataTypes.ITEM_STACK, "itemStack");
    }

}
