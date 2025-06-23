# Script Bundles

Script bundles are a set of files which contain scripted map content, you can imagine them similar to a
resource pack (infact one output of a script bundle _is_ a resource pack).

* The `/sketching` directory contains some sketches of what a script bundle might look like.
* The `/schema` directory contains in-progress JSON scemas for script bundle files.

## Bundle structure

Script bundles are structured similarly to resource packs:

```
/manifest.json
```

## Entity models

We support animated java as well as a variation of bedrock entity models.

### Animated Java

* Bones
* Variants
* Animations

### Bedrock

* Geometry
* Textures
* Animations

We will skip render controllers for now.

We will skip animation controllers for now. It will be possible to trigger animations via scripting only.

