package org.squiddev.cctweaks.mixins;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import javax.annotation.Nonnull;

public enum TargetMods implements ITargetMod {
    // Read the Javadoc of ITargetMod and TargetModBuilder for further information
    // Add to this enum information about the mods you need to identify during runtime
    COMPUTERCRAFT("dan200.computercraft.ComputerCraft", "ComputerCraft"),
    OPENPERIPHERALADDONS("openperipheral.addons.OpenPeripheralAddons", "OpenPeripheral"),
    OPENPERIPHERALCORE("openperipheral.OpenPeripheralCore", "OpenPeripheralCore"),
    MULTIPART("codechicken.multipart.minecraft.MinecraftMultipartMod", "McMultipart");

    private final TargetModBuilder builder;

    TargetMods(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass).setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
