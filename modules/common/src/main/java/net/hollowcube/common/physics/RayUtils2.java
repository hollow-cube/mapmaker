package net.hollowcube.common.physics;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public final class RayUtils2 {

    public static boolean boundingBoxContainsPoint(@NotNull BoundingBox bb, @NotNull Point bbRelative, @NotNull Point other) {
        return other.x() >= bb.minX() + bbRelative.x() && other.x() <= bb.maxX() + bbRelative.x()
                && other.y() >= bb.minY() + bbRelative.y() && other.y() <= bb.maxY() + bbRelative.y()
                && other.z() >= bb.minZ() + bbRelative.z() && other.z() <= bb.maxZ() + bbRelative.z();
    }

    public static boolean BoundingBoxIntersectionCheck(Point rayStart, Point rayDirection, BoundingBox collidableStatic, Point staticCollidableOffset, SweepResult2 finalResult) {
        // Translate bounding box
        Vec bbOffMin = new Vec(collidableStatic.minX() - rayStart.x() + staticCollidableOffset.x(), collidableStatic.minY() - rayStart.y() + staticCollidableOffset.y(), collidableStatic.minZ() - rayStart.z() + staticCollidableOffset.z());
        Vec bbOffMax = new Vec(collidableStatic.maxX() - rayStart.x() + staticCollidableOffset.x(), collidableStatic.maxY() - rayStart.y() + staticCollidableOffset.y(), collidableStatic.maxZ() - rayStart.z() + staticCollidableOffset.z());

        // This check is done in 2d. it can be visualised as a rectangle (the face we are checking), and a point.
        // If the point is within the rectangle, we know the vector intersects the face.

        double signumRayX = Math.signum(rayDirection.x());
        double signumRayY = Math.signum(rayDirection.y());
        double signumRayZ = Math.signum(rayDirection.z());

        boolean isHit = false;
        double percentage = finalResult.res;
        int collisionFace = -1;

        // Intersect X
        // Left side of bounding box
        if (rayDirection.x() > 0) {
            double xFac = epsilon(bbOffMin.x() / rayDirection.x());
            if (xFac < percentage) {
                double yix = rayDirection.y() * xFac + rayStart.y();
                double zix = rayDirection.z() * xFac + rayStart.z();

                // Check if ray passes through y/z plane
                if (((yix - rayStart.y()) * signumRayY) >= 0
                        && ((zix - rayStart.z()) * signumRayZ) >= 0
                        && yix >= collidableStatic.minY() + staticCollidableOffset.y()
                        && yix <= collidableStatic.maxY() + staticCollidableOffset.y()
                        && zix >= collidableStatic.minZ() + staticCollidableOffset.z()
                        && zix <= collidableStatic.maxZ() + staticCollidableOffset.z()) {
                    isHit = true;
                    percentage = xFac;
                    collisionFace = 0;
                }
            }
        }
        // Right side of bounding box
        if (rayDirection.x() < 0) {
            double xFac = epsilon(bbOffMax.x() / rayDirection.x());
            if (xFac < percentage) {
                double yix = rayDirection.y() * xFac + rayStart.y();
                double zix = rayDirection.z() * xFac + rayStart.z();

                if (((yix - rayStart.y()) * signumRayY) >= 0
                        && ((zix - rayStart.z()) * signumRayZ) >= 0
                        && yix >= collidableStatic.minY() + staticCollidableOffset.y()
                        && yix <= collidableStatic.maxY() + staticCollidableOffset.y()
                        && zix >= collidableStatic.minZ() + staticCollidableOffset.z()
                        && zix <= collidableStatic.maxZ() + staticCollidableOffset.z()) {
                    isHit = true;
                    percentage = xFac;
                    collisionFace = 0;
                }
            }
        }

        // Intersect Z
        if (rayDirection.z() > 0) {
            double zFac = epsilon(bbOffMin.z() / rayDirection.z());
            if (zFac < percentage) {
                double xiz = rayDirection.x() * zFac + rayStart.x();
                double yiz = rayDirection.y() * zFac + rayStart.y();

                if (((yiz - rayStart.y()) * signumRayY) >= 0
                        && ((xiz - rayStart.x()) * signumRayX) >= 0
                        && xiz >= collidableStatic.minX() + staticCollidableOffset.x()
                        && xiz <= collidableStatic.maxX() + staticCollidableOffset.x()
                        && yiz >= collidableStatic.minY() + staticCollidableOffset.y()
                        && yiz <= collidableStatic.maxY() + staticCollidableOffset.y()) {
                    isHit = true;
                    percentage = zFac;
                    collisionFace = 1;
                }
            }
        }
        if (rayDirection.z() < 0) {
            double zFac = epsilon(bbOffMax.z() / rayDirection.z());
            if (zFac < percentage) {
                double xiz = rayDirection.x() * zFac + rayStart.x();
                double yiz = rayDirection.y() * zFac + rayStart.y();

                if (((yiz - rayStart.y()) * signumRayY) >= 0
                        && ((xiz - rayStart.x()) * signumRayX) >= 0
                        && xiz >= collidableStatic.minX() + staticCollidableOffset.x()
                        && xiz <= collidableStatic.maxX() + staticCollidableOffset.x()
                        && yiz >= collidableStatic.minY() + staticCollidableOffset.y()
                        && yiz <= collidableStatic.maxY() + staticCollidableOffset.y()) {
                    isHit = true;
                    percentage = zFac;
                    collisionFace = 1;
                }
            }
        }

        // Intersect Y
        if (rayDirection.y() > 0) {
            double yFac = epsilon(bbOffMin.y() / rayDirection.y());
            if (yFac < percentage) {
                double xiy = rayDirection.x() * yFac + rayStart.x();
                double ziy = rayDirection.z() * yFac + rayStart.z();

                if (((ziy - rayStart.z()) * signumRayZ) >= 0
                        && ((xiy - rayStart.x()) * signumRayX) >= 0
                        && xiy >= collidableStatic.minX() + staticCollidableOffset.x()
                        && xiy <= collidableStatic.maxX() + staticCollidableOffset.x()
                        && ziy >= collidableStatic.minZ() + staticCollidableOffset.z()
                        && ziy <= collidableStatic.maxZ() + staticCollidableOffset.z()) {
                    isHit = true;
                    percentage = yFac;
                    collisionFace = 2;
                }
            }
        }

        if (rayDirection.y() < 0) {
            double yFac = epsilon(bbOffMax.y() / rayDirection.y());
            if (yFac < percentage) {
                double xiy = rayDirection.x() * yFac + rayStart.x();
                double ziy = rayDirection.z() * yFac + rayStart.z();

                if (((ziy - rayStart.z()) * signumRayZ) >= 0
                        && ((xiy - rayStart.x()) * signumRayX) >= 0
                        && xiy >= collidableStatic.minX() + staticCollidableOffset.x()
                        && xiy <= collidableStatic.maxX() + staticCollidableOffset.x()
                        && ziy >= collidableStatic.minZ() + staticCollidableOffset.z()
                        && ziy <= collidableStatic.maxZ() + staticCollidableOffset.z()) {
                    isHit = true;
                    percentage = yFac;
                    collisionFace = 2;
                }
            }
        }

        percentage *= 0.99999;

        if (isHit && percentage >= 0 && percentage <= finalResult.res) {
            finalResult.res = percentage;
            finalResult.normalX = 0;
            finalResult.normalY = 0;
            finalResult.normalZ = 0;

            if (collisionFace == 0) finalResult.normalX = 1;
            if (collisionFace == 1) finalResult.normalZ = 1;
            if (collisionFace == 2) finalResult.normalY = 1;

            finalResult.collidedPositionX = rayStart.x() + rayDirection.x() * percentage;
            finalResult.collidedPositionY = rayStart.y() + rayDirection.y() * percentage;
            finalResult.collidedPositionZ = rayStart.z() + rayDirection.z() * percentage;

            return true;
        }

        return false;
    }

    private static double epsilon(double value) {
        return Math.abs(value) < Vec.EPSILON ? 0 : value;
    }
}
