package me.kall.entitychunkdata.data;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntitiesInChunkData {
    public static final Map<ResourceLocation, Map<ChunkPos, Set<UUID>>> ENTITIES = new ConcurrentHashMap<>();
    private static boolean registered = false;

    public static @NotNull Iterator<Entity> getEntitiesInChunk(@NotNull ServerLevel level, ChunkPos pos) {
        Set<UUID> entitySet = new ObjectOpenHashSet<>(ENTITIES.getOrDefault(level.dimension().location(), Collections.emptyMap()).getOrDefault(pos, Collections.emptySet()));

        Iterator<UUID> backingIterator = entitySet.iterator();

        return new Iterator<>() {
            private Entity nextValid = null;
            private boolean hasNextEvaluated = false;

            private void findNextValid() {
                while (backingIterator.hasNext()) {
                    UUID candidateId = backingIterator.next();
                    Entity candidate = level.getEntity(candidateId);
                    if (candidate != null) {
                        nextValid = candidate;
                        return;
                    } else {
                        backingIterator.remove();
                    }
                }
                nextValid = null;
            }

            @Override
            public boolean hasNext() {
                if (!hasNextEvaluated) {
                    findNextValid();
                    hasNextEvaluated = true;
                }
                return nextValid != null;
            }

            @Override
            public Entity next() {
                if (!hasNext()) throw new NoSuchElementException();
                hasNextEvaluated = false;
                return nextValid;
            }
        };
    }

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
        entitySet.remove(entity.getUUID());

        if (!entitySet.isEmpty()) return;

        entitiesInChunk.remove(chunkPos);
        if (!entitiesInChunk.isEmpty()) return;

        ENTITIES.remove(dim);
    }

    @ApiStatus.Internal
    public static void addEntity(@NotNull ServerLevel level, @NotNull Entity entity) {
        if (level.isLoaded(entity.blockPosition()) && entity.isAlive()) {
            ChunkPos pos = entity.chunkPosition();
            ResourceLocation dim = level.dimension().location();
            ENTITIES
                    .computeIfAbsent(dim, key -> map())
                    .computeIfAbsent(pos, key -> set())
                    .add(entity.getUUID());
        }
    }

    public static void register() {
        if (registered) return;
        registered = true;
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EntitiesInChunkData::onChunkUnLoad);
        bus.addListener(EntitiesInChunkData::onLevelUnLoad);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onJoin);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onLeave);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onDie);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onTravel);
        bus.addListener(EventPriority.LOWEST, EntitiesInChunkData::onDespawn);
        bus.addListener(EntitiesInChunkData::onShutdown);
    }

    private static void onChunkUnLoad(ChunkEvent.@NotNull Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            Map<ChunkPos, Set<UUID>> entitiesInChunk = ENTITIES.get(level.dimension().location());
            if (entitiesInChunk == null) return;
            entitiesInChunk.remove(chunk.getPos());
        }
    }

    private static void onLevelUnLoad(LevelEvent.@NotNull Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ENTITIES.remove(level.dimension().location());
        }
    }

    private static void onJoin(@NotNull EntityJoinLevelEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level) {
            addEntity(level, event.getEntity());
        }
    }

    private static void onLeave(@NotNull EntityLeaveLevelEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level) {
            removeEntity(event.getEntity(), level);
        }
    }

    private static void onDie(@NotNull LivingDeathEvent event) {
        if (event.isCanceled()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            removeEntity(entity, level);
        }
    }

    private static void onTravel(@NotNull EntityTravelToDimensionEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level) {
            removeEntity(event.getEntity(), level);
        }
    }

    private static void onDespawn(MobSpawnEvent.@NotNull AllowDespawn event) {
        if (event.getResult().equals(Event.Result.DENY)) return;
        if (event.getEntity().level() instanceof ServerLevel level) {
            removeEntity(event.getEntity(), level);
        }
    }

    private static void onShutdown(ServerStoppingEvent event) {
        ENTITIES.clear();
    }
}