package org.squiddev.cctweaks.mixins.late;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cctweaks.api.network.INetworkAccess;
import org.squiddev.cctweaks.api.peripheral.IPeripheralEnvironments;

import openperipheral.adapter.composed.MethodSelector;
import openperipheral.interfaces.cc.ModuleComputerCraft;

@Mixin(ModuleComputerCraft.class)
public abstract class ModuleComputerCraft_Mixin {

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private static void injectNetworkEnv(CallbackInfo ci) {
        try {
            Field factoryField = ModuleComputerCraft.class.getDeclaredField("PERIPHERAL_METHODS_FACTORY");
            factoryField.setAccessible(true);
            Object factory = factoryField.get(null);
            if (factory == null) return;

            // Get 'composer' field from factory
            Field composerField = null;
            Class<?> clazz = factory.getClass();
            while (clazz != null) {
                try {
                    composerField = clazz.getDeclaredField("composer");
                    break;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (composerField == null) return;
            composerField.setAccessible(true);
            Object composer = composerField.get(factory);
            if (composer == null) return;

            // Find 'selector' field in composer
            Field selectorField = null;
            Class<?> selectorClass = composer.getClass();
            while (selectorClass != null) {
                try {
                    selectorField = selectorClass.getDeclaredField("selector");
                    break;
                } catch (NoSuchFieldException ignored) {
                    selectorClass = selectorClass.getSuperclass();
                }
            }
            if (selectorField == null) return;
            selectorField.setAccessible(true);
            MethodSelector selector = (MethodSelector) selectorField.get(composer);
            if (selector == null) return;
            selector.addProvidedEnv(IPeripheralEnvironments.ARG_NETWORK, INetworkAccess.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject network environment", e);
        }
    }
}
