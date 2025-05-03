package com.lartsal.autosell;

import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;

import java.util.List;

public class Utils {
    public static List<String> getSimpleParticleIds() {
        return Registries.PARTICLE_TYPE.stream()
                .filter(particleType -> particleType instanceof SimpleParticleType)
                .map(particleType -> Registries.PARTICLE_TYPE.getId(particleType).toString())
                .sorted()
                .toList();
    }
}
