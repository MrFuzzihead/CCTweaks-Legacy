package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import openperipheral.adapter.composed.ClassMethodsComposer;
import openperipheral.adapter.composed.ComposedMethodsFactory;

@Mixin(value = ComposedMethodsFactory.class, remap = false)
public interface ComposedMethodsFactory_Accessor {

    @Accessor("composer")
    ClassMethodsComposer getComposer();
}
