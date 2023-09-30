def _genfile_impl(ctx):
    ctx.actions.expand_template(
        template = ctx.file.template,
        output = ctx.outputs.output,
        substitutions = ctx.attr.substitutions,
    )
    return [
        DefaultInfo(files = depset([ctx.outputs.output])),
    ]

genfile = rule(
    implementation = _genfile_impl,
    attrs = {
        "template": attr.label(
            mandatory = True,
            allow_single_file = True,
        ),
        "output": attr.output(
            mandatory = True,
        ),
        "substitutions": attr.string_dict(),
    },
)
