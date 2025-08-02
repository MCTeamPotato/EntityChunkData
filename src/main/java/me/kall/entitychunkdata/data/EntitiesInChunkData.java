package me.kall.entitychunkdata.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntitiesInChunkData {
    public static final Map<ResourceLocation, Map<ChunkPos, Set<UUID>>> ENTITIES = new ConcurrentHashMap<>();
    public static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    @ApiStatus.Internal
    public static @NotNull Map<ChunkPos, Set<UUID>> map() {
        return new ConcurrentHashMap<>();
    }

    @ApiStatus.Internal
    public static @NotNull Set<UUID> set() {
        return ConcurrentHashMap.newKeySet();
    }

    @ApiStatus.Internal
    public static void removeEntity(@NotNull Entity entity, @NotNull ServerLevel level) {
        ChunkPos chunkPos = entity.chunkPosition();
        ResourceLocation dim = level.dimension().location();

        Map<ChunkPos, Set<UUID>> entitiesInChunk = ENTITIES.get(dim);
        if (entitiesInChunk == null) return;

        Set<UUID> entitySet = entitiesInChunk.get(chunkPos);
        if (entitySet == null) return;

        TASKS.add(() -> {
            entitySet.remove(entity.getUUID());

            if (!entitySet.isEmpty()) return;

            entitiesInChunk.remove(chunkPos);
            if (!entitiesInChunk.isEmpty()) return;
            ENTITIES.remove(dim);
        });
    }

    @ApiStatus.Internal
    public static void addEntity(@NotNull ServerLevel level, @NotNull Entity entity) {
        if (level.isLoaded(entity.blockPosition()) && entity.isAlive()) {
            ChunkPos pos = entity.chunkPosition();
            ResourceLocation dim = level.dimension().location();
            TASKS.add(() -> ENTITIES
                    .computeIfAbsent(dim, key -> map())
                    .computeIfAbsent(pos, key -> set())
                    .add(entity.getUUID()));
        }
    }

    public static void register() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EntitiesInChunkData::onChunkUnLoad);
        bus.addListener(EntitiesInChunkData::onLevelUnLoad);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onJoin);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onLeave);
    }

    private static void onChunkUnLoad(ChunkEvent.@NotNull Unload event) {
        if (event.getWorld() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            Map<ChunkPos, Set<UUID>> entitiesInChunk = ENTITIES.get(level.dimension().location());
            if (entitiesInChunk == null) return;
            entitiesInChunk.remove(chunk.getPos());
        }
    }

    private static void onLevelUnLoad(WorldEvent.@NotNull Unload event) {
        if (event.getWorld() instanceof ServerLevel level) {
            ENTITIES.remove(level.dimension().location());
        }
    }

    private static void onJoin(@NotNull EntityJoinWorldEvent event) {
        if (!event.isCanceled() && event.getEntity().level instanceof ServerLevel level) {
            addEntity(level, event.getEntity());
        }
    }

    private static void onLeave(@NotNull EntityLeaveWorldEvent event) {
        if (event.getEntity().level instanceof ServerLevel level) {
            removeEntity(event.getEntity(), level);
        }
    }
}