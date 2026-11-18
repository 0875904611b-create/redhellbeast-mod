package com.redhellbeast.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Builds the "inside the NES cartridge" boss arena: a black/red checkered disc ringed by lava, pillars and skulls. */
public final class ArenaBuilder {
    public static final int FLOOR_Y = 64;
    public static final int FLOOR_R = 32;
    private static final int LAVA_R = 35;
    private static final int WALL_R = 37;
    private static final int HEIGHT = 40;

    public static void ensureBuilt(ServerLevel level) {
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) level.setChunkForced(cx, cz, true);
        }
        boolean built = level.getBlockState(new BlockPos(0, FLOOR_Y - 2, 0)).is(Blocks.CRYING_OBSIDIAN);
        buildFloor(level);   // idempotent: also repairs leftover lava patches after a crash/reload
        if (!built) buildStructure(level);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, 2);
    }

    private static void buildFloor(ServerLevel level) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState redBricks = Blocks.RED_NETHER_BRICKS.defaultBlockState();
        BlockState cry = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        for (int x = -WALL_R; x <= WALL_R; x++) {
            for (int z = -WALL_R; z <= WALL_R; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= FLOOR_R) {
                    BlockState top = d > FLOOR_R - 1.5 ? cry : ((((x >> 2) + (z >> 2)) & 1) == 0 ? blackstone : redBricks);
                    set(level, p.set(x, FLOOR_Y, z), top);
                    set(level, p.set(x, FLOOR_Y - 1, z), blackstone);
                    set(level, p.set(x, FLOOR_Y - 2, z), (x == 0 && z == 0) ? cry : bedrock);
                } else if (d <= LAVA_R) {
                    set(level, p.set(x, FLOOR_Y, z), Blocks.LAVA.defaultBlockState());
                    set(level, p.set(x, FLOOR_Y - 1, z), Blocks.NETHERRACK.defaultBlockState());
                    set(level, p.set(x, FLOOR_Y - 2, z), bedrock);
                }
            }
        }
    }

    private static void buildStructure(ServerLevel level) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        for (int x = -WALL_R - 2; x <= WALL_R + 2; x++) {
            for (int z = -WALL_R - 2; z <= WALL_R + 2; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > LAVA_R && d <= WALL_R) {
                    for (int y = FLOOR_Y - 2; y <= FLOOR_Y + 3; y++) set(level, p.set(x, y, z), wall);
                }
                if (d > WALL_R && d <= WALL_R + 1.6) {
                    for (int y = FLOOR_Y - 2; y <= FLOOR_Y + HEIGHT; y++) set(level, p.set(x, y, z), barrier);
                }
                if (d <= WALL_R + 1.6) set(level, p.set(x, FLOOR_Y + HEIGHT, z), barrier);
            }
        }
        // pillars (cover from the beam) topped with glowing shroomlight and skulls
        for (int k = 0; k < 8; k++) {
            double ang = k * Math.PI / 4 + Math.PI / 8;
            int px = (int) Math.round(Math.cos(ang) * 22), pz = (int) Math.round(Math.sin(ang) * 22);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 && dz != 0) continue;
                    for (int y = 1; y <= 9; y++) set(level, p.set(px + dx, FLOOR_Y + y, pz + dz), Blocks.OBSIDIAN.defaultBlockState());
                    set(level, p.set(px + dx, FLOOR_Y + 10, pz + dz), Blocks.SHROOMLIGHT.defaultBlockState());
                }
            }
            set(level, p.set(px, FLOOR_Y + 11, pz), Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, k * 2));
        }
        // ring of skulls + soul campfires on the arena rim
        for (int i = 0; i < 60; i++) {
            double ang = i * Math.PI * 2 / 60;
            int sx = (int) Math.round(Math.cos(ang) * (FLOOR_R - 1)), sz = (int) Math.round(Math.sin(ang) * (FLOOR_R - 1));
            Block b = (i % 5 == 0) ? Blocks.WITHER_SKELETON_SKULL : Blocks.SKELETON_SKULL;
            set(level, p.set(sx, FLOOR_Y + 1, sz), b.defaultBlockState().setValue(SkullBlock.ROTATION, Mth.clamp(i % 16, 0, 15)));
        }
        for (int i = 0; i < 20; i++) {
            double ang = i * Math.PI * 2 / 20 + 0.1;
            int sx = (int) Math.round(Math.cos(ang) * (FLOOR_R - 4)), sz = (int) Math.round(Math.sin(ang) * (FLOOR_R - 4));
            set(level, p.set(sx, FLOOR_Y + 1, sz), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        }
        set(level, new BlockPos(0, FLOOR_Y - 2, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState());
    }

    private ArenaBuilder() {}
}
