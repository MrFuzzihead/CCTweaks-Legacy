package org.squiddev.cctweaks.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TileBaseLifecycleTest {

    private static final class TestTile extends TileBase {

        int created;
        int destroyed;

        @Override
        public void create() {
            created++;
        }

        @Override
        public void destroy() {
            destroyed++;
        }
    }

    @Test
    public void validateCreatesAndClearsInvalidFlag() {
        TestTile tile = new TestTile();

        tile.validate();
        assertEquals(1, tile.created);
        assertFalse(tile.isInvalid());

        tile.invalidate();
        assertTrue(tile.isInvalid());

        tile.validate();
        assertFalse(tile.isInvalid());
        assertEquals(2, tile.created);
    }

    @Test
    public void destroyIsIdempotentAcrossLifecycleCallbacks() {
        TestTile tile = new TestTile();
        tile.validate();

        tile.invalidate();
        assertEquals(1, tile.destroyed);

        tile.invalidate();
        tile.onChunkUnload();
        assertEquals(1, tile.destroyed);
    }

    @Test
    public void chunkUnloadAlsoDestroysOnce() {
        TestTile tile = new TestTile();
        tile.validate();

        tile.onChunkUnload();
        assertEquals(1, tile.destroyed);

        tile.invalidate();
        assertEquals(1, tile.destroyed);
    }

    @Test
    public void explicitDestroyOnceDoesNotRepeat() {
        TestTile tile = new TestTile();
        tile.validate();

        tile.destroyOnce();
        tile.destroyOnce();
        assertEquals(1, tile.destroyed);
    }

    @Test
    public void markForUpdateWithoutWorldIsSafe() {
        TestTile tile = new TestTile();
        tile.markForUpdate();
    }

    @Test
    public void defaultDescriptionPacketIsNull() {
        TestTile tile = new TestTile();
        assertNull(tile.getDescriptionPacket());
    }
}
