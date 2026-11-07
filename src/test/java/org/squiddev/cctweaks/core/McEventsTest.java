package org.squiddev.cctweaks.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.squiddev.cctweaks.core.utils.WorldPosition;

public class McEventsTest {

    private final McEvents events = new McEvents();
    private final List<ItemStack> consumed = new ArrayList<ItemStack>();
    private final McEvents.IDropConsumer consumer = consumed::add;

    @BeforeEach
    public void resetBefore() {
        McEvents.reset();
        consumed.clear();
    }

    @AfterEach
    public void resetAfter() {
        McEvents.reset();
    }

    private static World worldWithRandom() throws Exception {
        World world = Mockito.mock(World.class);
        Field rand = World.class.getField("rand");
        rand.set(world, new Random(0));
        return world;
    }

    @Test
    public void blockConsumerReceivesDropsAndClearsList() throws Exception {
        World world = worldWithRandom();
        Block block = Mockito.mock(Block.class);
        ItemStack stack = new ItemStack(new Item());
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        drops.add(stack);

        BlockEvent.HarvestDropsEvent event = new BlockEvent.HarvestDropsEvent(
            1,
            2,
            3,
            world,
            block,
            0,
            0,
            1.0f,
            drops,
            null,
            false);

        McEvents.addBlockConsumer(new WorldPosition(world, 1, 2, 3), consumer);
        events.onHarvestDrops(event);

        assertEquals(Collections.singletonList(stack), consumed);
        assertTrue(drops.isEmpty());
    }

    @Test
    public void blockConsumerIgnoresOtherPositions() throws Exception {
        World world = worldWithRandom();
        Block block = Mockito.mock(Block.class);
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        drops.add(new ItemStack(new Item()));

        BlockEvent.HarvestDropsEvent event = new BlockEvent.HarvestDropsEvent(
            1,
            2,
            3,
            world,
            block,
            0,
            0,
            1.0f,
            drops,
            null,
            false);

        McEvents.addBlockConsumer(new WorldPosition(world, 9, 9, 9), consumer);
        events.onHarvestDrops(event);

        assertTrue(consumed.isEmpty());
        assertFalse(drops.isEmpty());
    }

    @Test
    public void entityConsumerReceivesDropsAndIsRemoved() {
        EntityLivingBase entity = Mockito.mock(EntityLivingBase.class);
        EntityItem item = Mockito.mock(EntityItem.class);
        ItemStack stack = new ItemStack(new Item());
        Mockito.when(item.getEntityItem())
            .thenReturn(stack);

        ArrayList<EntityItem> drops = new ArrayList<EntityItem>();
        drops.add(item);
        LivingDropsEvent event = new LivingDropsEvent(entity, null, drops, 0, true, 0);

        McEvents.addEntityConsumer(entity, consumer);
        assertTrue(entity.captureDrops);

        events.onEntityLivingDrops(event);

        assertEquals(Collections.singletonList(stack), consumed);
        assertTrue(event.drops.isEmpty());

        McEvents.removeEntityConsumer(entity);
        assertFalse(entity.captureDrops);
    }

    @Test
    public void cancelledEntityDropsAreNotConsumed() {
        EntityLivingBase entity = Mockito.mock(EntityLivingBase.class);
        EntityItem item = Mockito.mock(EntityItem.class);
        Mockito.when(item.getEntityItem())
            .thenReturn(new ItemStack(new Item()));

        ArrayList<EntityItem> drops = new ArrayList<EntityItem>();
        drops.add(item);
        LivingDropsEvent event = new CancellableLivingDropsEvent(entity, null, drops, 0, true, 0);
        event.setCanceled(true);

        McEvents.addEntityConsumer(entity, consumer);
        events.onEntityLivingDrops(event);

        assertTrue(consumed.isEmpty());
        assertFalse(event.drops.isEmpty());
    }

    private static final class CancellableLivingDropsEvent extends LivingDropsEvent {

        private CancellableLivingDropsEvent(EntityLivingBase entity, net.minecraft.util.DamageSource source,
            ArrayList<EntityItem> drops, int lootingLevel, boolean recentlyHit, int specialDropValue) {
            super(entity, source, drops, lootingLevel, recentlyHit, specialDropValue);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    @Test
    public void resetClearsConsumersAndCaptureFlag() {
        EntityLivingBase entity = Mockito.mock(EntityLivingBase.class);

        McEvents.addBlockConsumer(new WorldPosition(Mockito.mock(World.class), 0, 0, 0), consumer);
        McEvents.addEntityConsumer(entity, consumer);
        assertTrue(entity.captureDrops);

        McEvents.reset();
        assertFalse(entity.captureDrops);
    }
}
