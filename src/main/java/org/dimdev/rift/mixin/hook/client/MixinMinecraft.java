package org.dimdev.rift.mixin.hook.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackInfoClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.ResourcePackList;
import org.dimdev.rift.listener.ClientTickable;
import org.dimdev.rift.listener.ResourcePackFinderAdder;
import org.dimdev.simpleloader.SimpleLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Shadow @Final private ResourcePackList<ResourcePackInfoClient> mcResourcePackRepository;

    @Shadow @Final public Profiler mcProfiler;
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourcePackList;addPackFinder(Lnet/minecraft/resources/IPackFinder;)V", ordinal = 1))
    private void onAddResourcePacks(ResourcePackList resourcePackList, IPackFinder minecraftPackFinder) {
        resourcePackList.addPackFinder(minecraftPackFinder);

        for (ResourcePackFinderAdder resourcePackFinderAdder : SimpleLoader.instance.getListeners(ResourcePackFinderAdder.class)) {
            for (IPackFinder packFinder : resourcePackFinderAdder.getResourcePackFinders()) {
                mcResourcePackRepository.addPackFinder(packFinder);
            }
        }
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        mcProfiler.startSection("tickMods");
        for (ClientTickable tickable : SimpleLoader.instance.getListeners(ClientTickable.class)) {
            mcProfiler.startSection(() -> tickable.getClass().getCanonicalName().replace('.', '/'));
            tickable.clientTick();
            mcProfiler.endSection();
        }
        mcProfiler.endSection();
    }
}
