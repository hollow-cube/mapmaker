package net.hollowcube.terraform.legacy.mask.script;

public sealed interface MaybeMask permits MaybeMask.Mask, MaybeMask.Error {

    record Mask(net.hollowcube.terraform.legacy.mask.Mask mask) implements MaybeMask {
    }

    record Error(MaskParseException error) implements MaybeMask {
    }

}
