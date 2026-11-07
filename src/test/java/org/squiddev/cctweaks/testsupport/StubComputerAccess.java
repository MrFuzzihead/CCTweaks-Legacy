package org.squiddev.cctweaks.testsupport;

import java.util.ArrayList;
import java.util.List;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;

/**
 * An {@link IComputerAccess} that records queued events instead of talking to a computer.
 */
public class StubComputerAccess implements IComputerAccess {

    public static final class Event {

        public final String name;
        public final Object[] arguments;

        public Event(String name, Object[] arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }

    public final List<Event> events = new ArrayList<Event>();
    private final int id;
    private final String attachmentName;

    public StubComputerAccess(int id, String attachmentName) {
        this.id = id;
        this.attachmentName = attachmentName;
    }

    @Override
    public String mount(String name, IMount mount) {
        return name;
    }

    @Override
    public String mount(String name, IMount mount, String driveName) {
        return name;
    }

    @Override
    public String mountWritable(String name, IWritableMount mount) {
        return name;
    }

    @Override
    public String mountWritable(String name, IWritableMount mount, String driveName) {
        return name;
    }

    @Override
    public void unmount(String name) {}

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void queueEvent(String name, Object[] arguments) {
        events.add(new Event(name, arguments));
    }

    @Override
    public String getAttachmentName() {
        return attachmentName;
    }
}
