/**
 * {@link net.hollowcube.terraform.selection.region.Region}s are a way to select a group of block positions in a world.
 *
 * <p>{@link net.hollowcube.terraform.selection.region.Region}s are defined by a {@link net.hollowcube.terraform.selection.region.RegionSelector}
 * which is responsible for creating the regions via primary and secondary selections from the user (/pos1, /pos2, etc). The selector
 * is in charge of setting up the client renderer, but not in charge of deciding whether anything will really be sent to the player.</p>
 *
 * @see net.hollowcube.terraform.TerraformModule#regionTypes()
 */
package net.hollowcube.terraform.selection.region;
