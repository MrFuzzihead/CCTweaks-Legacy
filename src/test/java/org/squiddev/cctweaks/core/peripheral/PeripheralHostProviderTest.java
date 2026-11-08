package org.squiddev.cctweaks.core.peripheral;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.squiddev.cctweaks.api.peripheral.IPeripheralHost;

import dan200.computercraft.api.peripheral.IPeripheral;

public class PeripheralHostProviderTest {

    private static final class HostTile extends TileEntity implements IPeripheralHost {

        private final IPeripheral peripheral;
        private int lastSide = Integer.MIN_VALUE;

        private HostTile(IPeripheral peripheral) {
            this.peripheral = peripheral;
        }

        @Override
        public IPeripheral getPeripheral(int side) {
            lastSide = side;
            return peripheral;
        }
    }

    @Test
    public void hostPeripheralIsReturnedForTheRequestedSide() {
        IPeripheral peripheral = Mockito.mock(IPeripheral.class);
        HostTile tile = new HostTile(peripheral);
        World world = Mockito.mock(World.class);
        Mockito.when(world.getTileEntity(1, 2, 3))
            .thenReturn(tile);

        PeripheralHostProvider provider = new PeripheralHostProvider();
        assertSame(peripheral, provider.getPeripheral(world, 1, 2, 3, 4));
        assertSame(4, tile.lastSide);
    }

    @Test
    public void tilesThatAreNotHostsAreIgnored() {
        World world = Mockito.mock(World.class);
        Mockito.when(world.getTileEntity(1, 2, 3))
            .thenReturn(new TileEntity());

        assertNull(new PeripheralHostProvider().getPeripheral(world, 1, 2, 3, 0));
    }

    @Test
    public void emptyPositionsAreIgnored() {
        World world = Mockito.mock(World.class);
        Mockito.when(world.getTileEntity(1, 2, 3))
            .thenReturn(null);

        assertNull(new PeripheralHostProvider().getPeripheral(world, 1, 2, 3, 0));
    }

    @Test
    public void hostsMayDeclineASide() {
        HostTile tile = new HostTile(null);
        World world = Mockito.mock(World.class);
        Mockito.when(world.getTileEntity(0, 0, 0))
            .thenReturn(tile);

        assertNull(new PeripheralHostProvider().getPeripheral(world, 0, 0, 0, 2));
        assertSame(2, tile.lastSide);
    }
}
