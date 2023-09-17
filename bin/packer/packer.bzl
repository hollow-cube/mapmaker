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

#def _packer_bundle_impl(ctx):
#    out = ctx.actions.declare_file(ctx.label.name)
#
#    ctx.actions.run(
#        inputs = ctx.files.srcs,
#        outputs = [out],
#        arguments = [],
#        progress_message = "RUNNING PACKER",
#        executable = ctx.executable.packer_binary,
#    )
#
#    return [DefaultInfo(files = depset([out]))]
#
#packer_bundle = rule(
#    implementation = _packer_bundle_impl,
#    attrs = {
#        "srcs": attr.label_list(allow_files = True),
#        "packer_binary": attr.label(
#            executable = True,
#            cfg = "exec",
#            default = Label("//bin/packer:packer"),
#        ),
#    },
#)

# def _impl(ctx):
#    # The list of arguments we pass to the script.
#    args = [ctx.outputs.out.path] + [f.path for f in ctx.files.chunks]
#
#    # Action to call the script.
#    ctx.actions.run(
#        inputs = ctx.files.chunks,
#        outputs = [ctx.outputs.out],
#        arguments = args,
#        progress_message = "Merging into %s" % ctx.outputs.out.short_path,
#        executable = ctx.executable.merge_tool,
#    )
#
#concat = rule(
#    implementation = _impl,
#    attrs = {
#        "chunks": attr.label_list(allow_files = True),
#        "out": attr.output(mandatory = True),
#        "merge_tool": attr.label(
#            executable = True,
#            cfg = "exec",
#            allow_files = True,
#            default = Label("//actions_run:merge"),
#        ),
#    },
#)
