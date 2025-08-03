package me.kall.entitychunkdata.mixin;

import me.kall.entitychunkdata.data.EntitiesInChunkData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "updateChunkPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;removeEntity(Lnet/minecraft/world/entity/Entity;I)V"))
    private void onRemove(Entity entity, CallbackInfo ci) {
        EntitiesInChunkData.removeEntity(entity, (ServerLevel) (Object) this);
    }

    @Inject(method = "updateChunkPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;addEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void onAdd(Entity entity, CallbackInfo ci) {
        EntitiesInChunkData.addEntity((ServerLevel) (Object) this, entity);
    }
}
