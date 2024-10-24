package net.hollowcube.terraform.compat.axiom.world.property;

import net.hollowcube.terraform.util.ProtocolUtil;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;

import java.nio.charset.StandardCharsets;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
final class DataTypes {

    static final class BooleanType implements DataType<Boolean> {
        @Override
        public int typeId() {
            return 0;
        }

        @Override
        public byte[] serialize(Boolean value) {
            return new byte[]{value ? (byte) 1 : (byte) 0};
        }

        @Override
        public Boolean deserialize(byte[] value) {
            return value[0] != 0;
        }
    }

    static final class IntegerType implements DataType<Integer> {
        @Override
        public int typeId() {
            return 1;
        }

        @Override
        public byte[] serialize(Integer value) {
            return ProtocolUtil.makeArray(5, buffer -> buffer.write(VAR_INT, value));
        }

        @Override
        public Integer deserialize(byte[] value) {
            // TODO(1.21.2)
//            return new NetworkBuffer(ByteBuffer.wrap(value)).read(VAR_INT);
            return null;
        }
    }

    static final class StringType implements DataType<String> {
        @Override
        public int typeId() {
            return 2;
        }

        @Override
        public byte[] serialize(String value) {
            return value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String deserialize(byte[] value) {
            return new String(value, StandardCharsets.UTF_8);
        }
    }

    static final class MaterialType implements DataType<Material> {
        @Override
        public int typeId() {
            return 3;
        }

        @Override
        public byte[] serialize(Material value) {
            return ProtocolUtil.makeArray(5, buffer -> buffer.write(VAR_INT, value.id()));
        }

        @Override
        public Material deserialize(byte[] value) {
//            return Material.fromId(new NetworkBuffer(ByteBuffer.wrap(value)).read(VAR_INT));
            // TODO(1.21.2)
            return null;
        }
    }

    static final class BlockType implements DataType<Block> {
        @Override
        public int typeId() {
            return 4;
        }

        @Override
        public byte[] serialize(Block value) {
            return ProtocolUtil.makeArray(5, buffer -> buffer.write(VAR_INT, (int) value.stateId()));
        }

        @Override
        public Block deserialize(byte[] value) {
//            return Block.fromStateId(new NetworkBuffer(ByteBuffer.wrap(value)).read(VAR_INT).shortValue());
            return null;
        }
    }

    static final class EmptyType implements DataType<Void> {
        @Override
        public int typeId() {
            return 5;
        }

        @Override
        public byte[] serialize(Void value) {
            return new byte[0];
        }

        @Override
        public Void deserialize(byte[] value) {
            return null;
        }
    }


}
