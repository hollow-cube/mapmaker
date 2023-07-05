package net.hollowcube.terraform.cui;

public record ColorScheme(int primary, int secondary) {
    public static final ColorScheme DEFAULT = new ColorScheme(0xFFFF0000, 0x66FF0000);
}
