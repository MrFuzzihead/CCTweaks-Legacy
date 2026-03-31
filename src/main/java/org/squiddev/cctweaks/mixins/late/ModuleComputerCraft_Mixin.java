package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cctweaks.api.network.INetworkAccess;
import org.squiddev.cctweaks.api.peripheral.IPeripheralEnvironments;

import openperipheral.adapter.composed.ClassMethodsComposer;
import openperipheral.adapter.composed.MethodSelector;
import openperipheral.interfaces.cc.ModuleComputerCraft;

@Mixin(ModuleComputerCraft.class)
public abstract class ModuleComputerCraft_Mixin {

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private static void injectNetworkEnv(CallbackInfo ci) {
        if (ModuleComputerCraft.PERIPHERAL_METHODS_FACTORY == null) return;

        ClassMethodsComposer composer = ((ComposedMethodsFactory_Accessor) ModuleComputerCraft.PERIPHERAL_METHODS_FACTORY)
            .getComposer();
        if (composer == null) return;

        MethodSelector selector = (MethodSelector) ((ClassMethodsComposer_Accessor) composer).getSelector();
        if (selector == null) return;

        selector.addProvidedEnv(IPeripheralEnvironments.ARG_NETWORK, INetworkAccess.class);
    }
}
