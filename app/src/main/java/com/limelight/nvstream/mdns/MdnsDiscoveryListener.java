package com.moonlight.nvstream.mdns;

public interface MdnsDiscoveryListener {
    void notifyComputerAdded(MdnsComputer computer);
    void notifyDiscoveryFailure(Exception e);
}
