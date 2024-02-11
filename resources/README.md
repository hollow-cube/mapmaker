# MapMaker Resources

Eventually will be processed into the resource pack for mapmaker.
Contains some metadata for client & server, the processing step will be used to split the files into a set for client
and server.

- Client will be a resource pack, published to a cdn
- Server will be included either in the server jar or the server docker image.
    - Metadata about images

`_client` is a resource pack structure, used as the base for the resource pack (anything static can go there)

In theory, we can generate a lot more if desired. For example, it is possible to look at the usages of
each `TextElement`
and compute the offset required to sit at the given pixel position based on the X/Y position of the node.
Similarly, we can compute the origin and shift values of the textures used based on their node positions.

If both of the above are done, it would be fairly easy to create an editor for GUIs i think.

# Recipe book tabs

The effect works similarly to how we do 2d in gui/3d in hand items. In the model we have one layer
for the "base texture" (redstone, for example. this is the tab item we want to change), and then a
plane behind the base texture which is the intended tab icon.

Problem 1: How to detect the overlay vs the base texture?

-> We use the alpha value of the texture (for example, the base texture has an alpha of 238) which we can
detect inside the shader. Once we have found the particular alpha in question, we can remove that
component and set it back to 1. This is the same as 2d/3d items, and handled by the outer if/else if below.

Problem 2: How to detect if we are rendering a recipe book tab icon?

-> Unfortunately we cannot use z values to detect the recipe book because it is rendered at the same Z
as the rest of the GUIs (like creative menu). Instead, we use the X value of the position. The recipe book
is further to the left than the rest of the GUIs
TODO: THIS DOES NOT WORK ON ALL GUI SCALES CURRENTLY, NEED TO FIX.
More info about this methodology can be found here:
https://github.com/McTsts/mc-core-shaders/blob/main/gui%20scale/util.glsl

