package org.squiddev.cctweaks.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

public interface IDataCard {

    String EMPTY = "cctweaks.data.empty";

    void setSettings(ItemStack var1, String var2, NBTTagCompound var3);

    String getType(ItemStack var1);

    NBTTagCompound getData(ItemStack var1);

    void notifyPlayer(EntityPlayer var1, IDataCard.Messages var2);

    public static enum Messages {

        Loaded,
        Stored,
        Cleared;

        public IChatComponent getChatMessage() {
            return new ChatComponentTranslation("chat.cctweaks.data.messages." + this.toString(), new Object[0]);
        }
    }
}
