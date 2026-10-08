package com.andrey.anomalnayazhara;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnomnayaZhara implements ModInitializer {
    private static final Map<UUID, Integer> HEAT = new HashMap<>();
    private static final Map<UUID, Integer> THIRST = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerWorld world = player.getEntityWorld();

                long day = world.getTimeOfDay() / 24000L;
                int baseHeat = (int)MathHelper.clamp(day * 2, 0, 100);
                int heat = HEAT.getOrDefault(player.getUuid(), 20);
                int thirst = THIRST.getOrDefault(player.getUuid(), 100);

                boolean sunlight = world.isSkyVisible(player.getBlockPos().up());
                if (sunlight) heat += 1;
                else heat -= 1;

                if (player.getMainHandStack().isOf(Items.WATER_BUCKET)
                        || player.getOffHandStack().isOf(Items.WATER_BUCKET)) heat -= 2;

                heat = MathHelper.clamp(Math.max(heat, baseHeat), 0, 100);
                if (sunlight && heat > 65 && world.getTime() % 40 == 0) thirst--;

                if (world.getTime() % 100 == 0) {
                    player.sendMessage(Text.literal("§c☀ Аномальная жара: §f" + heat + "°C §7| §b💧 Жажда: §f" + Math.max(thirst, 0) + "%"), true);
                }

                if (sunlight && heat >= 75 && world.getTime() % 40 == 0) {
                    player.damage(world, world.getDamageSources().onFire(), 1.0f);
                }

                if (world.getTime() % 20 == 0 && heat >= 40) {
                    world.spawnParticles(ParticleTypes.FALLING_WATER,
                            player.getX(), player.getY() + 1.8, player.getZ(),
                            2, 0.25, 0.15, 0.25, 0.01);
                }

                HEAT.put(player.getUuid(), heat);
                THIRST.put(player.getUuid(), Math.max(thirst, 0));

                if (day >= 3 && world.getTime() % 200 == 0) {
                    turnNearbyGroundToSand(world, player.getBlockPos(), day);
                }
            }
        });
    }

    private static void turnNearbyGroundToSand(ServerWorld world, BlockPos center, long day) {
        int radius = (int)MathHelper.clamp(2 + day * 2, 2, 12);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.add(x, -1, z);
                var block = world.getBlockState(pos).getBlock();
                if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT) {
                    world.setBlockState(pos, Blocks.SAND.getDefaultState());
                }
            }
        }
    }
}
