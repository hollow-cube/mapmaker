# packer_bundle adds relevant rules for packer client and server data.
def packer_bundle(name, srcs, visibility = None):
    native.genrule(
        name = "packer_%s_server" % name,
        srcs = srcs,
        outs = [
            "fonts.json",
            "en_US.json",
            "sprites.json",
        ],
        cmd = "python3 bin/packer/build_server.py '$(location //bin/packer:packer)' $(OUTS)",
        tools = ["//bin/packer"],
        visibility = visibility,
    )

    native.genrule(
        name = "packer_%s_client" % name,
        srcs = srcs,
        outs = ["client.zip"],
        cmd = "python3 bin/packer/build_client.py '$(location //bin/packer:packer)' $(OUTS)",
        tools = ["//bin/packer"],
        visibility = visibility,
    )
