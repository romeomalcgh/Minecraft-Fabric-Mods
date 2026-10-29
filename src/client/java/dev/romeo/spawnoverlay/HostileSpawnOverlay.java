package dev.romeo.spawnoverlay;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.ArrayList;
import java.util.List;

public final class HostileSpawnOverlay implements ClientModInitializer {
    private static final int DEFAULT_RADIUS = 8;
    private static final int DEFAULT_SCAN_INTERVAL_TICKS = 4;
    private static final int MAX_RADIUS = 24;
    private static final int MIN_SCAN_INTERVAL_TICKS = 1;
    private static final int MAX_SCAN_INTERVAL_TICKS = 200;
    private static final int HOSTILE_SKY_LIGHT_LIMIT = 7;
    private static final int NIGHT_SKY_DARKEN = 11;

    private static final int RED = 0xFFFF2020;
    private static final int YELLOW = 0xFFFFD000;
    private static final int GREEN = 0xFF20FF50;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hostile_spawn_overlay", "general"));
    private static KeyMapping toggleKey;
    private static boolean enabled = false;
    private static int radius = DEFAULT_RADIUS;
    private static int scanIntervalTicks = DEFAULT_SCAN_INTERVAL_TICKS;
    private static int tickCounter = 0;
    private static final List<Marker> MARKERS = new ArrayList<>();

    private record Marker(BlockPos pos, double topHeight, int color) {}

    @Override
    public void onInitializeClient() {
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hostile_spawn_overlay.toggle", InputConstants.Type.KEYBOARD, 79, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(HostileSpawnOverlay::onClientTick);
        LevelRenderEvents.COLLECT_SUBMITS.register(HostileSpawnOverlay::renderMarkers);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("hso")
                        .then(ClientCommands.literal("radius")
                                .then(ClientCommands.argument("blocks", IntegerArgumentType.integer(1, MAX_RADIUS))
                                        .executes(context -> {
                                            radius = IntegerArgumentType.getInteger(context, "blocks");
                                            MARKERS.clear();
                                            context.getSource().sendFeedback(Component.literal(
                                                    "Hostile Spawn Overlay radius set to " + radius + " blocks."));
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("interval")
                                .then(ClientCommands.argument("ticks", IntegerArgumentType.integer(
                                                MIN_SCAN_INTERVAL_TICKS, MAX_SCAN_INTERVAL_TICKS))
                                        .executes(context -> {
                                            scanIntervalTicks = IntegerArgumentType.getInteger(context, "ticks");
                                            tickCounter = 0;
                                            context.getSource().sendFeedback(Component.literal(
                                                    "Hostile Spawn Overlay scan interval set to " + scanIntervalTicks
                                                            + " ticks (" + String.format("%.2f", scanIntervalTicks / 20.0)
                                                            + " seconds)."));
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("status").executes(context -> {
                            context.getSource().sendFeedback(Component.literal(
                                    "Hostile Spawn Overlay: " + (enabled ? "ON" : "OFF")
                                            + " | radius=" + radius + " blocks | interval=" + scanIntervalTicks
                                            + " ticks"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("reset").executes(context -> {
                            radius = DEFAULT_RADIUS;
                            scanIntervalTicks = DEFAULT_SCAN_INTERVAL_TICKS;
                            tickCounter = 0;
                            MARKERS.clear();
                            context.getSource().sendFeedback(Component.literal(
                                    "Hostile Spawn Overlay settings reset to defaults (radius 8, interval 4 ticks)."));
                            return 1;
                        }))));
    }

    private static void onClientTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            if (!enabled) MARKERS.clear();
        }
        if (!enabled || client.player == null || client.level == null) return;
        if (++tickCounter % scanIntervalTicks != 0) return;

        ClientLevel level = client.level;
        BlockPos center = client.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Vec3 playerPos = client.player.position();
        int minY = Math.max(level.getMinY() + 1, center.getY() - 4);
        int maxY = Math.min(level.getMaxY() - 2, center.getY() + 4);
        List<Marker> nextMarkers = new ArrayList<>();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                double dx = x + 0.5 - playerPos.x;
                double dz = z + 0.5 - playerPos.z;
                if (dx * dx + dz * dz > radius * radius) continue;

                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;

                    BlockState floor = level.getBlockState(pos);
                    if (floor.isAir() || !floor.getFluidState().isEmpty()) continue;
                    var shape = floor.getCollisionShape(level, pos);
                    if (shape.isEmpty()) continue;
                    double topHeight = shape.max(Direction.Axis.Y);

                    BlockPos spawnPos = pos.above();
                    BlockPos headPos = spawnPos.above();
                    BlockState feet = level.getBlockState(spawnPos);
                    BlockState head = level.getBlockState(headPos);
                    if (!feet.getCollisionShape(level, spawnPos).isEmpty()
                            || !head.getCollisionShape(level, headPos).isEmpty()) continue;
                    if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) continue;

                    int color;
                    if (!isValidHostileFloor(level, pos, floor)) {
                        color = GREEN;
                    } else {
                        int blockLight = level.getBrightness(LightLayer.BLOCK, spawnPos);
                        int skyLight = level.getBrightness(LightLayer.SKY, spawnPos);

                        // Evaluate both hypothetical conditions, not just the current clock time.
                        // Day: no sky darkening. Night: subtract the typical sky-darkening value.
                        boolean daySpawnable = blockLight == 0 && skyLight <= HOSTILE_SKY_LIGHT_LIMIT;
                        int nightSkyLight = Math.max(0, skyLight - NIGHT_SKY_DARKEN);
                        boolean nightSpawnable = blockLight == 0 && nightSkyLight <= HOSTILE_SKY_LIGHT_LIMIT;

                        if (daySpawnable && nightSpawnable) color = RED;
                        else if (!daySpawnable && nightSpawnable) color = YELLOW;
                        else color = GREEN;
                    }

                    nextMarkers.add(new Marker(new BlockPos(x, y, z), topHeight, color));
                }
            }
        }

        MARKERS.clear();
        MARKERS.addAll(nextMarkers);
    }

    private static void renderMarkers(LevelRenderContext context) {
        if (!enabled || MARKERS.isEmpty()) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        var poseStack = context.poseStack();
        poseStack.pushPose();
        try {
            for (Marker marker : MARKERS) {
                // A thin, colored box on the block's top face. This is real world geometry,
                // not a particle, so it stays crisp and does not fade away.
                poseStack.pushPose();
                poseStack.translate(marker.pos().getX() - camera.x,
                        marker.pos().getY() + marker.topHeight() - camera.y + 0.012,
                        marker.pos().getZ() - camera.z);
                context.submitNodeCollector().submitShapeOutline(
                        poseStack,
                        Shapes.box(0.025, 0.0, 0.025, 0.975, 0.018, 0.975),
                        RenderTypes.lines(),
                        marker.color(),
                        3.0f,
                        true);
                poseStack.popPose();
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static boolean isValidHostileFloor(ClientLevel level, BlockPos pos, BlockState floor) {
        return floor.isValidSpawn(level, pos,
                BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace("zombie")).orElseThrow())
                || floor.isValidSpawn(level, pos,
                BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace("skeleton")).orElseThrow())
                || floor.isValidSpawn(level, pos,
                BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace("creeper")).orElseThrow())
                || floor.isValidSpawn(level, pos,
                BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace("spider")).orElseThrow())
                || floor.isValidSpawn(level, pos,
                BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace("witch")).orElseThrow());
    }
}
