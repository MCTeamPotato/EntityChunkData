package me.kall.entitychunkdata.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
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
        ChunkPos pos = entity.chunkPosition();
        ResourceLocation dim = level.dimension().location();
        TASKS.add(() -> ENTITIES
                .computeIfAbsent(dim, key -> map())
                .computeIfAbsent(pos, key -> set())
                .add(entity.getUUID()));
    }

    public static void register() {
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(EntitiesInChunkData::onChunkUnLoad);
        bus.addListener(EntitiesInChunkData::onLevelUnLoad);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onJoin);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onLeave);
    }

    private static void onChunkUnLoad(ChunkEvent.@NotNull Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            Map<ChunkPos, Set<UUID>> entitiesInChunk = ENTITIES.get(level.dimension().location());
            if (entitiesInChunk == null) return;
            TASKS.add(() -> entitiesInChunk.remove(chunk.getPos()));
        }
    }

    private static void onLevelUnLoad(LevelEvent.@NotNull Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TASKS.add(() -> ENTITIES.remove(level.dimension().location()));
        }
    }

    private static void onJoin(@NotNull EntityJoinLevelEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level) {
            addEntity(level, event.getEntity());
        }
    }

    private static void onLeave(@NotNull EntityLeaveLevelEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            removeEntity(event.getEntity(), level);
        }
    }
}