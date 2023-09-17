# packer_bundle adds relevant rules for packer client and server data.
def packer_bundle(name, srcs):
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
    )
