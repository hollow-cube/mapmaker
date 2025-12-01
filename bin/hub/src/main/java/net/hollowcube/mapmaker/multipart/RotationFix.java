package net.hollowcube.mapmaker.multipart;


import net.hollowcube.mapmaker.multipart.animatedjava.AnimatedJavaBlueprint;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Either;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Bedrock model cubes are allowed to have rotations that are not valid item models.
 *
 * <p>To get around this, we replace those cases with a new bone which has the rotation
 * (rather than the item model).</p>
 */
public final class RotationFix {
    // TODO can we just use new item model rotation to do this instead?

    public static AnimatedJavaBlueprint fixBoneRotation(AnimatedJavaBlueprint blueprint) {
        var badCubes = new HashMap<String, AnimatedJavaBlueprint.Element.Cube>();

        var newElements = new ArrayList<AnimatedJavaBlueprint.Element>();
        for (var element : blueprint.elements()) {
            if (!(element instanceof AnimatedJavaBlueprint.Element.Cube cube) || isValidRotation(cube.rotation())) {
                newElements.add(element);
                continue;
            }

            var newCube = new AnimatedJavaBlueprint.Element.Cube(
                cube.transform(),
                cube.uuid(),
                cube.name(),
                cube.origin(),
                cube.from(),
                cube.to(),
                Vec.ZERO,
                cube.uvOffset(),
                cube.inflate(),
                cube.faces(),
                cube.rescale(),
                cube.autoUv(),
                cube.boxUv()
            );
            newElements.add(newCube);
            badCubes.put(cube.uuid(), cube);
        }

        System.out.println("bad cubes are " + badCubes);

        class OutlineFixer {
            public static AnimatedJavaBlueprint.OutlineElement fix(Map<String, AnimatedJavaBlueprint.Element.Cube> badCubes, AnimatedJavaBlueprint.OutlineElement element, Vec parentOrigin) {
                List<Either<String, AnimatedJavaBlueprint.OutlineElement>> newChildren = new ArrayList<>();

                for (var child : element.children()) {
                    if (!(child instanceof Either.Left(var id))) {
                        newChildren.add(new Either.Right<>(fix(badCubes,
                            ((Either.Right<String, AnimatedJavaBlueprint.OutlineElement>) child).value(),
                            element.origin().asVec())));
                        continue;
                    }
                    if (!badCubes.containsKey(id)) {
                        newChildren.add(child);
                        continue;
                    }

                    // wrap it with another element
                    var wrapper = new AnimatedJavaBlueprint.OutlineElement(
                        "wrapper_" + id,
                        "wrapper_" + id,
                        // preserve the origin so that when we subtract from parent its correct.
                        badCubes.get(id).origin(),
                        badCubes.get(id).rotation(),
                        Collections.singletonList(child)
                    );
                    newChildren.add(new Either.Right<>(wrapper));
                }

                return new AnimatedJavaBlueprint.OutlineElement(
                    element.uuid(),
                    element.name(),
                    element.origin(),
                    element.rotation(),
                    newChildren
                );
            }
        }

        var newOutliner = OutlineFixer.fix(badCubes, new AnimatedJavaBlueprint.OutlineElement(
            "root", "root", Vec.ZERO, Vec.ZERO, blueprint.outliner()
        ), Vec.ZERO).children();


        return new AnimatedJavaBlueprint(
            blueprint.meta(),
            blueprint.resolution(),
            newElements,
            blueprint.textures(),
            newOutliner,
            blueprint.animations()
        );
    }

    private static boolean isValidRotation(@Nullable Point rotation) {
        if (rotation == null || rotation.isZero()) return true;

        // if we have multiple axes, its invalid
        if (rotation.x() != 0 && rotation.y() != 0) return false;
        if (rotation.x() != 0 && rotation.z() != 0) return false;
        if (rotation.y() != 0 && rotation.z() != 0) return false;

        // all rotations must be -45, -22.5, 0, 22.5, or 45
        if (rotation.x() != 0 && rotation.x() != -45 && rotation.x() != -22.5 && rotation.x() != 22.5 && rotation.x() != 45)
            return false;
        if (rotation.y() != 0 && rotation.y() != -45 && rotation.y() != -22.5 && rotation.y() != 22.5 && rotation.y() != 45)
            return false;
        if (rotation.z() != 0 && rotation.z() != -45 && rotation.z() != -22.5 && rotation.z() != 22.5 && rotation.z() != 45)
            return false;

        return true;
    }
}
