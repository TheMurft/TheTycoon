package dev.themurft.thetycoon.model;

import org.bukkit.Color;
import org.bukkit.Material;

public record MinionDefinition(String id, Material output, Material block, Material tool,
                               double buyPrice, double sellPrice, double baseInterval,
                               int order, Color armorColor) {
}
