package dev.themurft.thetycoon.model;

import org.bukkit.Material;

public enum MinionType {
    COBBLESTONE(Material.COBBLESTONE, Material.COBBLESTONE, Material.WOODEN_PICKAXE),
    STONE(Material.STONE, Material.STONE, Material.STONE_PICKAXE),
    COAL(Material.COAL, Material.COAL_BLOCK, Material.STONE_PICKAXE),
    COPPER(Material.RAW_COPPER, Material.COPPER_BLOCK, Material.STONE_PICKAXE),
    IRON(Material.IRON_INGOT, Material.IRON_BLOCK, Material.IRON_PICKAXE),
    GOLD(Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.GOLDEN_PICKAXE),
    REDSTONE(Material.REDSTONE, Material.REDSTONE_BLOCK, Material.IRON_PICKAXE),
    LAPIS(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK, Material.IRON_PICKAXE),
    DIAMOND(Material.DIAMOND, Material.DIAMOND_BLOCK, Material.DIAMOND_PICKAXE),
    EMERALD(Material.EMERALD, Material.EMERALD_BLOCK, Material.DIAMOND_PICKAXE),
    QUARTZ(Material.QUARTZ, Material.QUARTZ_BLOCK, Material.IRON_PICKAXE),
    NETHERITE(Material.NETHERITE_SCRAP, Material.NETHERITE_BLOCK, Material.NETHERITE_PICKAXE),
    OBSIDIAN(Material.OBSIDIAN, Material.OBSIDIAN, Material.DIAMOND_PICKAXE),
    NETHERRACK(Material.NETHERRACK, Material.NETHERRACK, Material.STONE_PICKAXE),
    END_STONE(Material.END_STONE, Material.END_STONE, Material.IRON_PICKAXE),
    SAND(Material.SAND, Material.SANDSTONE, Material.WOODEN_SHOVEL),
    GRAVEL(Material.GRAVEL, Material.GRAVEL, Material.STONE_SHOVEL),
    CLAY(Material.CLAY_BALL, Material.CLAY, Material.IRON_SHOVEL),
    DIRT(Material.DIRT, Material.DIRT, Material.WOODEN_SHOVEL),
    OAK(Material.OAK_LOG, Material.OAK_WOOD, Material.WOODEN_AXE),
    SPRUCE(Material.SPRUCE_LOG, Material.SPRUCE_WOOD, Material.STONE_AXE),
    BIRCH(Material.BIRCH_LOG, Material.BIRCH_WOOD, Material.STONE_AXE),
    JUNGLE(Material.JUNGLE_LOG, Material.JUNGLE_WOOD, Material.IRON_AXE),
    ACACIA(Material.ACACIA_LOG, Material.ACACIA_WOOD, Material.IRON_AXE),
    DARK_OAK(Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD, Material.DIAMOND_AXE),
    BAMBOO(Material.BAMBOO, Material.BAMBOO_BLOCK, Material.WOODEN_AXE),
    CACTUS(Material.CACTUS, Material.CACTUS, Material.WOODEN_HOE),
    WHEAT(Material.WHEAT, Material.HAY_BLOCK, Material.WOODEN_HOE),
    CARROT(Material.CARROT, Material.ORANGE_CONCRETE, Material.STONE_HOE),
    POTATO(Material.POTATO, Material.BROWN_CONCRETE, Material.STONE_HOE);

    private final Material output;
    private final Material block;
    private final Material tool;
    MinionType(Material output, Material block, Material tool) { this.output=output; this.block=block; this.tool=tool; }
    public Material output(){return output;} public Material block(){return block;} public Material tool(){return tool;}
}
