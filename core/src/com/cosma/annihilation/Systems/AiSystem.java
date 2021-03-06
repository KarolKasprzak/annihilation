package com.cosma.annihilation.Systems;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.World;
import com.cosma.annihilation.Components.*;
import com.cosma.annihilation.EntityEngine.core.ComponentMapper;
import com.cosma.annihilation.EntityEngine.core.Engine;
import com.cosma.annihilation.EntityEngine.core.Entity;
import com.cosma.annihilation.EntityEngine.core.Family;
import com.cosma.annihilation.EntityEngine.systems.IteratingSystem;
import com.cosma.annihilation.Utils.Constants;


public class AiSystem extends IteratingSystem {
    private World world;
    private ComponentMapper<AiComponent> aiMapper;
    private ComponentMapper<AnimationComponent> animationMapper;
    private ComponentMapper<PhysicsComponent> bodyMapper;
    private ComponentMapper<HealthComponent> healthMapper;
    private BitmapFont font;
    private SpriteBatch batch;
    private Camera camera;
    private OrthographicCamera worldCamera;

    public AiSystem(World world, SpriteBatch batch, OrthographicCamera camera) {
        super(Family.all(AiComponent.class).get(), Constants.AI_SYSTEM);
        this.world = world;
        this.batch = batch;
        this.worldCamera = camera;
        aiMapper = ComponentMapper.getFor(AiComponent.class);
        animationMapper = ComponentMapper.getFor(AnimationComponent.class);
        bodyMapper = ComponentMapper.getFor(PhysicsComponent.class);
        healthMapper = ComponentMapper.getFor(HealthComponent.class);
        font = new BitmapFont();
        font.setColor(Color.RED);
        font.getData().setScale(1, 1);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        camera = this.getEngine().getSystem(UserInterfaceSystem.class).getStage().getCamera();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AiComponent aiComponent = aiMapper.get(entity);
        PhysicsComponent physicsComponent = bodyMapper.get(entity);
        if(!aiComponent.isPaused){
            aiComponent.task.update(entity,deltaTime);
        }


//        AnimationComponent animationComponent = animationMapper.get(entity);
//        HealthComponent healthComponent = healthMapper.get(entity);
//
//        if(!healthComponent.isDead){
//            aiComponent.ai.update(entity);
//        }else physicsComponent.body.setLinearVelocity(new Vector2(0, physicsComponent.body.getLinearVelocity().y));
//
//        Vector3 worldPosition = worldCamera.project(new Vector3(physicsComponent.body.getPosition().x,physicsComponent.body.getPosition().y+1,0));
//        batch.setProjectionMatrix(camera.combined);
//        batch.begin();
//        font.draw(batch, aiComponent.ai.getStatus(), worldPosition.x, worldPosition.y);
//        batch.end();


    }
}
