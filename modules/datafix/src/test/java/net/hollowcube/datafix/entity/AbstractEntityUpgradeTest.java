package net.hollowcube.datafix.entity;

import net.hollowcube.datafix.AbstractTypedUpgradeTest;
import net.hollowcube.datafix.DataTypes;

abstract class AbstractEntityUpgradeTest extends AbstractTypedUpgradeTest {

    public AbstractEntityUpgradeTest() {
        super(DataTypes.ENTITY, "entity");
    }

}
