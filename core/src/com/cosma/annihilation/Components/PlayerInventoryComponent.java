package com.cosma.annihilation.Components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Array;
import com.cosma.annihilation.Items.Item;

public class PlayerInventoryComponent implements Component {
    public Array<Item> equippedItem = new Array<>();
    public Array<Item> inventoryItem = new Array<>();
}
