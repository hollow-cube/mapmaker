package net.hollowcube.terraform.mask.script;

public sealed interface MaybeMask permits MaybeMask.Mask, MaybeMask.Error {

    record Mask(net.hollowcube.terraform.mask.Mask mask) implements MaybeMask {}

    record Error(MaskParseException error) implements MaybeMask {}

}
