# React JS Lib

A Bazel module for downloading `react-reconciler`, `react`, and `scheduler` for use in the script engine.

### Updating the dependencies

After updating the `package.json` the following command can be used to update the lockfile, thus updating
the versions of the libraries downloaded by Bazel.

```shell
bazel run -- @pnpm//:pnpm --dir $PWD install --lockfile-only
```
