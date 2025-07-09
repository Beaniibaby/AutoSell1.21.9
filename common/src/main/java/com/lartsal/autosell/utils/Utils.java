package com.lartsal.autosell.utils;

import com.lartsal.autosell.datastructures.Point3D;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class Utils {
    private static final Random random = new Random();
    public static final Pattern TRADES_ENTRY_PATTERN = Pattern.compile("^\\s*(?:([a-z0-9_.-]+):)?([a-z0-9/._-]+)(?:\\s*\\+\\s*(?:([a-z0-9_.-]+):)?([a-z0-9/._-]+))?\\s*=>\\s*(?:([a-z0-9_.-]+):)?([a-z0-9/._-]+)\\s*$");

    public static int getRandomSign() {
        return random.nextBoolean() ? 1 : -1;
    }

    public static double getRandomDouble(double max) {
        if (max <= 0) {
            return 0;
        }
        return random.nextDouble(max);
    }

    @SuppressWarnings("DataFlowIssue")
    public static List<String> getSimpleParticleNames() {
        return BuiltInRegistries.PARTICLE_TYPE.stream()
                .filter(particleType -> particleType instanceof SimpleParticleType)
                .map(particleType -> BuiltInRegistries.PARTICLE_TYPE.getKey(particleType).toString())
                .sorted()
                .toList();
    }

    public static Point3D getRandomPointOnCuboid(double radiusX, double radiusY, double radiusZ) {
        double Sx = 2 * radiusY * radiusZ;
        double Sy = 2 * radiusX * radiusZ;
        double Sz = 2 * radiusX * radiusY;
        double Stotal = Sx + Sy + Sz;

        double u = Utils.getRandomDouble(Stotal);

        double x, y, z;

        if (u <= Sx) {
            x = Utils.getRandomSign() * radiusX;
            y = Utils.getRandomSign() * Utils.getRandomDouble(radiusY);
            z = Utils.getRandomSign() * Utils.getRandomDouble(radiusZ);
        } else if (u <= Sx + Sy) {
            x = Utils.getRandomSign() * Utils.getRandomDouble(radiusX);
            y = Utils.getRandomSign() * radiusY;
            z = Utils.getRandomSign() * Utils.getRandomDouble(radiusZ);
        } else {
            x = Utils.getRandomSign() * Utils.getRandomDouble(radiusX);
            y = Utils.getRandomSign() * Utils.getRandomDouble(radiusY);
            z = Utils.getRandomSign() * radiusZ;
        }

        return new Point3D(x, y, z);
    }

    public static Point3D getRandomPointOnEllipsoid(double radiusX, double radiusY, double radiusZ) {
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();

        double phi = 2 * Math.PI * u1;
        double theta = Math.acos(2 * u2 - 1);

        double x = radiusX * Math.sin(theta) * Math.cos(phi);
        double y = radiusY * Math.sin(theta) * Math.sin(phi);
        double z = radiusZ * Math.cos(theta);

        return new Point3D(x, y, z);
    }

    public static boolean isValidParticle(String particleName) {
        try {
            return BuiltInRegistries.PARTICLE_TYPE.containsKey(ResourceLocation.parse(particleName));
        } catch (ResourceLocationException e) {
            return false;
        }
    }

    public static boolean isValidTradeEntry(String entry) {
        return TRADES_ENTRY_PATTERN.matcher(entry).matches();
    }
}
