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
        cmd = """
        $(location //bin/packer:packer) out_here_hack
        echo "$(OUTS)" > output_files
        for path in $$(cat output_files); do
            file=$$(basename $$path)
            cp server/$$file $$path
        done
        """,
        tools = ["//bin/packer"],
        visibility = visibility,
    )

    native.genrule(
        name = "packer_%s_client" % name,
        srcs = srcs,
        outs = ["client.zip"],
        cmd = """
            $(location //bin/packer:packer)
            cd build/packer/client && zip -rq ../../../$(OUTS) .
        """,
        tools = ["//bin/packer"],
        visibility = visibility,
    )
