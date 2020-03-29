package com.cosma.annihilation.Gui;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.cosma.annihilation.Annihilation;
import com.cosma.annihilation.Components.PlayerComponent;
import com.cosma.annihilation.Components.PlayerInventoryComponent;
import com.cosma.annihilation.Items.InventorySlot;
import com.cosma.annihilation.Items.InventorySlotTarget;
import com.badlogic.gdx.utils.Array;
import com.cosma.annihilation.Items.Item;
import com.cosma.annihilation.Utils.Util;


public class ContainerWindow extends Window {
    public DragAndDrop dragAndDrop;
    public Table containerSlotsTable;
    private int itemSlotNumber;
    private TextButton takeAllButton;
    private TextButton closeButton;
    private ContainerWindow containerWindow;
    private ActorGestureListener listener;
    private Engine engine;
    private float guiScale;
    //todo
    private PlayerComponent playerComponent;

    public ContainerWindow(String title, Skin skin, int itemSlotNumber, final Engine engine) {
        super(title, skin);
        this.itemSlotNumber = itemSlotNumber;
        this.engine = engine;

        this.debugAll();
        containerWindow = this;
        this.background(new TextureRegionDrawable(new TextureRegion(Annihilation.getAssets().get("gfx/interface/gui_frame.png",Texture.class))));

        guiScale = 1.3f;

        dragAndDrop = new DragAndDrop();
        takeAllButton = new TextButton("Take all", skin);
        Util.setButtonColor(takeAllButton);
        closeButton = new TextButton("Close", skin);
        Util.setButtonColor(closeButton);
        listener = new ActorGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {
                super.tap(event, x, y, count, button);
                if(count >= 2){
                    if(((InventorySlot) event.getListenerActor()).hasItem()){
                        moveItemToPlayerEquipment((InventorySlot) event.getListenerActor());
                    }
                }
            }
        };

        createContainerTable();
    }

    private void moveItemToPlayerEquipment(InventorySlot equipmentSlot) {
        if (engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first().getComponent(PlayerInventoryComponent.class).inventoryItems.size <= 24) {
            engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first().getComponent(PlayerInventoryComponent.class).inventoryItems.add(equipmentSlot.getItem());
            equipmentSlot.clearItems();
        }
    }



    private int findEmptySlotInEquipment() {
        int n = 0;
        Array<Item> inventoryItem = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first().getComponent(PlayerInventoryComponent.class).inventoryItems;

        Array<Integer> numbers = new Array<>();

        for (Item item : inventoryItem) {
            numbers.add(item.getTableIndex());
        }

        for(int i = 0; i <= 24; i++){
            if(!numbers.contains(i,true)){
                n = i;
                break;
            }
        }
        return  n;
    }

    private void createContainerTable() {
        containerSlotsTable = new Table();
        containerSlotsTable.setDebug(false);
        containerSlotsTable.setFillParent(false);

        for (int i = 0; i < itemSlotNumber; i++) {
            InventorySlot inventorySlot = new InventorySlot();
            inventorySlot.addListener(listener);
            inventorySlot.setImageScale(1f);
            dragAndDrop.addTarget(new InventorySlotTarget(inventorySlot));
            containerSlotsTable.add(inventorySlot).size(Util.setWindowHeight(0.1f)*guiScale, Util.setWindowHeight(0.1f)*guiScale).pad(Util.setWindowHeight(0.005f));
            if (i == 6 || i == 12 || i == 18) containerSlotsTable.row();
        }

        this.add(containerSlotsTable).center().fillX().colspan(2).pad(80);
        this.row();
        this.add(takeAllButton).bottom().center().size(Util.setButtonWidth(1.7f), Util.setButtonHeight(1.7f));
        this.add(closeButton).bottom().center().size(Util.setButtonWidth(1.7f), Util.setButtonHeight(1.7f));
        closeButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                containerWindow.setVisible(false);
                return super.touchDown(event, x, y, pointer, button);
            }
        });
    }
}




