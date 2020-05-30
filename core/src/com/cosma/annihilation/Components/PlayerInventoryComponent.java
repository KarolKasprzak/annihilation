package com.cosma.annihilation.Components;

import com.cosma.annihilation.EntityEngine.core.Component;
import com.badlogic.gdx.utils.Array;
import com.cosma.annihilation.Items.Item;

public class PlayerInventoryComponent implements Component {
    public Item equippedArmour;
    public Item equippedWeapon;
    public Array<Item> inventoryItems = new Array<>();

}
