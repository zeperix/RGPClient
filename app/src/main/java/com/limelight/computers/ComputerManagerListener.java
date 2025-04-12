package com.moonlight.computers;

import com.moonlight.nvstream.http.ComputerDetails;

public interface ComputerManagerListener {
    void notifyComputerUpdated(ComputerDetails details);
}
