package utils;

import utils.Logger;

/**
 * This Class is used for get the art_quick_to_interpreter_bridge address
 * Do not call this forever!!!
 */
public class NeverCalled {
    private void fake(int a) {
        Logger.i(getClass().getSimpleName(), a + "Do not inline me!!");
    }
}
