package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.google.common.base.Predicate;

import openperipheral.adapter.IMethodExecutor;
import openperipheral.adapter.composed.ClassMethodsComposer;

@Mixin(value = ClassMethodsComposer.class, remap = false)
public interface ClassMethodsComposer_Accessor {

    @Accessor("selector")
    Predicate<IMethodExecutor> getSelector();
}
