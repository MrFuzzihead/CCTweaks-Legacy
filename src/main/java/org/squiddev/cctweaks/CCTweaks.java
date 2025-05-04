package org.squiddev.cctweaks;

import net.minecraft.creativetab.CreativeTabs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import dan200.computercraft.ComputerCraft;

@Mod(
    modid = CCTweaks.MODID,
    name = CCTweaks.NAME,
    version = CCTweaks.VERSION,
    dependencies = CCTweaks.DEPENDENCIES,
    guiFactory = CCTweaks.GUI_FACTORY)
public class CCTweaks {

    public static final String MODID = "cctweaks";
    public static final String NAME = "CCTweaks";
    public static final String VERSION = Tags.VERSION;
    public static final String RESOURCE_DOMAIN = MODID;
    public static final String DEPENDENCIES = "required-after:ComputerCraft@[1.74,);after:CCTurtle;after:ForgeMultipart;after:OpenPeripheralCore;";
    public static final String ROOT_NAME = "org.squiddev.cctweaks.";
    public static final String GUI_FACTORY = ROOT_NAME + "client.gui.GuiConfigFactory";

    public static CreativeTabs getCreativeTab() {
        return ComputerCraft.mainCreativeTab;
    }

    @SidedProxy(clientSide = "org.squiddev.cctweaks.ClientProxy", serverSide = "org.squiddev.cctweaks.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }
}
