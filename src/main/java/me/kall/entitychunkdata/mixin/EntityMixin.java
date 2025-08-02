package me.kall.entitychunkdata.mixin;

import me.kall.entitychunkdata.data.EntitiesInChunkData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow private Level level;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;createInsecureUUID(Lnet/minecraft/util/RandomSource;)Ljava/util/UUID;"), require = 0)
    private UUID initId(RandomSource randomSource) {
        UUID id = Mth.createInsecureUUID(randomSource);
        if (this.level instanceof ServerLevel serverLevel) {
            while (serverLevel.getEntity(id) != null) {
                id = Mth.createInsecureUUID(randomSource);
            }
        }
        return id;
    }

    @Inject(method = "setPosRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;<init>(Lnet/minecraft/core/BlockPos;)V"))
    private void chunkPosUpdatePre(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            EntitiesInChunkData.removeEntity(self, serverLevel);
        }
    }

    @Inject(method = "setPosRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;<init>(Lnet/minecraft/core/BlockPos;)V", shift = At.Shift.AFTER))
    private void chunkPosUpdatePost(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            EntitiesInChunkData.addEntity(serverLevel, self);
        }
    }
}