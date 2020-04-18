package com.cosma.annihilation.Systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.signals.Listener;
import com.badlogic.ashley.signals.Signal;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cosma.annihilation.Annihilation;
import com.cosma.annihilation.Components.PlayerComponent;
import com.cosma.annihilation.Gui.MainMenu.PlayerMenuWindow;
import com.cosma.annihilation.Utils.Constants;
import com.cosma.annihilation.Utils.EntityEngine;
import com.cosma.annihilation.Utils.Enums.GameEvent;


public class UserInterfaceSystem extends IteratingSystem implements Listener<GameEvent> {

    private Stage stage;
    private Label fpsLabel,onGround,canJump;
    private PlayerMenuWindow playerMainMenu;
    private ShaderProgram shader;
    private FrameBuffer fbo;


    public UserInterfaceSystem(EntityEngine engine) {
        super(Family.all(PlayerComponent.class).get(), Constants.USER_INTERFACE);
        //shader
        String vertexShader = Gdx.files.internal("shaders/scan_ver.glsl").readString();
        String fragmentShader = Gdx.files.internal("shaders/scan_frag.glsl").readString();
        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(vertexShader, fragmentShader);
        fbo = new FrameBuffer(Pixmap.Format.RGB888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        //shader end

        Skin skin = Annihilation.getAssets().get("gfx/interface/skin/skin.json", Skin.class);
        Camera camera = new OrthographicCamera();
        camera.update();
        Viewport viewport = new ScreenViewport();
        stage = new Stage(viewport);
        stage.getViewport().apply(true);

        Table coreTable = new Table();
        coreTable.setDebug(false);
        coreTable.setFillParent(true);
        stage.addActor(coreTable);

        playerMainMenu = new PlayerMenuWindow("", skin, engine);
        fpsLabel = new Label("", skin);
        onGround = new Label("", skin);
        canJump = new Label("", skin);

        coreTable.add(fpsLabel).left().top().expandX();
        coreTable.row();
        coreTable.add(onGround).left().top();
        coreTable.row();
        coreTable.add(canJump).left().top().expandY();
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        fpsLabel.setText(Float.toString(Gdx.graphics.getFramesPerSecond()));
        EntityEngine entityEngine = (EntityEngine) getEngine();
        if(entityEngine.getPlayerEntity() != null){
            PlayerComponent playerComponent = entityEngine.getPlayerEntity().getComponent(PlayerComponent.class);
            onGround.setText("on ground: " + playerComponent.onGround);
            canJump.setText("canJump: " + playerComponent.canJump);
        }
        Batch batch = stage.getBatch();
        stage.act(deltaTime);
        fbo.begin();
        stage.draw();
        fbo.end();


        stage.draw();


        if (playerMainMenu.isOpen()) {

            Texture texture = fbo.getColorBufferTexture();

            int x = (int) (playerMainMenu.getX() + (playerMainMenu.getWindowTable().getX()));
            int y = (int) (playerMainMenu.getY() + (playerMainMenu.getWindowTable().getY()));
            int w = (int) playerMainMenu.getWindowTable().getWidth();
            int h = (int) playerMainMenu.getWindowTable().getHeight();


            TextureRegion textureRegion = new TextureRegion(texture, x, y, w, h);
            textureRegion.flip(false, true);
            batch.setShader(shader);
            batch.begin();
            batch.draw(textureRegion, x, y, textureRegion.getRegionWidth(), textureRegion.getRegionHeight());
            batch.setShader(null);
            batch.end();
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {

    }

    void openPlayerMenu(boolean openLootMenu) {
        PlayerComponent playerComponent =  ((EntityEngine) getEngine()).getPlayerEntity().getComponent(PlayerComponent.class);
        if (stage.getActors().contains(playerMainMenu, true)) {
            playerMainMenu.close();
            playerMainMenu.setOpen(false);
            playerComponent.isPlayerControlEnable = true;
        } else {
            playerComponent.isPlayerControlEnable = false;
            stage.addActor(playerMainMenu);
            playerMainMenu.setOpen(true);
            if (openLootMenu) {
                playerMainMenu.openLootWindow();
            }
            playerMainMenu.moveToCenter();
        }
    }

    public void resizeHUD(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    public void receive(Signal<GameEvent> signal, GameEvent event) {
        if (event == GameEvent.OPEN_MENU) {
            openPlayerMenu(false);
        }
    }
}
