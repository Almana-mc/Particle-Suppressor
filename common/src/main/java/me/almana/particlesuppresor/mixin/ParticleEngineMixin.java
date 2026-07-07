package me.almana.particlesuppresor.mixin;

import me.almana.particlesuppresor.Rules;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(
        method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void particlesuppresor$suppress(ParticleOptions options, double x, double y, double z,
            double xd, double yd, double zd, CallbackInfoReturnable<Particle> cir) {
        if (Rules.suppress(options, x, y, z)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void particlesuppresor$suppressDestroy(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (Rules.suppress(ParticleTypes.BLOCK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
            ci.cancel();
        }
    }

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void particlesuppresor$suppressCrack(BlockPos pos, Direction side, CallbackInfo ci) {
        if (Rules.suppress(ParticleTypes.BLOCK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
            ci.cancel();
        }
    }
}
