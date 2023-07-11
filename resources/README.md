# MapMaker Resources

Eventually will be processed into the resource pack for mapmaker.
Contains some metadata for client & server, the processing step will be used to split the files into a set for client
and server.

- Client will be a resource pack, published to a cdn
- Server will be included either in the server jar or the server docker image.
    - Metadata about images

`_client` is a resource pack structure, used as the base for the resource pack (anything static can go there)


In theory, we can generate a lot more if desired. For example, it is possible to look at the usages of each `TextElement`
and compute the offset required to sit at the given pixel position based on the X/Y position of the node.
Similarly, we can compute the origin and shift values of the textures used based on their node positions.

If both of the above are done, it would be fairly easy to create an editor for GUIs I think.
