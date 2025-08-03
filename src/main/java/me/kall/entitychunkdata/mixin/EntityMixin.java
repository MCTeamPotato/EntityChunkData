package me.kall.entitychunkdata.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public Level level;
    @Shadow public abstract void setUUID(UUID uniqueId);

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void init(Entity instance, double x, double y, double z) {
        instance.setPos(x, y, z);
        if (this.level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) this.level;
            UUID id = Mth.createInsecureUUID(this.level.getRandom());
            while (serverLevel.getEntity(id) != null) id = Mth.createInsecureUUID(this.level.getRandom());
            this.setUUID(id);
        }
    }
}