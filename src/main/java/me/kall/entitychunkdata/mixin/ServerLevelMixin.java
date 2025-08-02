package me.kall.entitychunkdata.mixin;

import me.kall.entitychunkdata.data.EntitiesInChunkData;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickEnd(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        Runnable task;
        while ((task = EntitiesInChunkData.TASKS.poll()) != null) {
            task.run();
        }
    }
}
