package com.goosethings.tools.client.animation;

/** Explicit marker on this GUI's frozen render state; never set on world entities. */
public interface CelebrationState {
    void goosetools$celebration(int action, float seconds);
    int goosetools$action();
    float goosetools$seconds();
}
