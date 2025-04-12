package com.moonlight.ui;

import com.moonlight.binding.input.GameInputDevice;

public interface GameGestures {
    void toggleKeyboard();

    default void showGameMenu(GameInputDevice device){};
}
