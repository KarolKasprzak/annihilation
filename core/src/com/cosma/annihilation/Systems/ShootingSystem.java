package com.cosma.annihilation.Systems;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.signals.Listener;
import com.badlogic.ashley.signals.Signal;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cosma.annihilation.Annihilation;
import com.cosma.annihilation.Components.*;
import com.cosma.annihilation.Entities.EntityFactory;
import com.cosma.annihilation.Items.Item;
import com.cosma.annihilation.Items.ItemType;
import com.cosma.annihilation.Utils.Constants;
import com.cosma.annihilation.Utils.Animation.AnimationStates;
import com.cosma.annihilation.Utils.CollisionID;
import com.cosma.annihilation.Utils.EntityEngine;
import com.cosma.annihilation.Utils.Enums.GameEvent;
import com.esotericsoftware.spine.Bone;

import java.util.concurrent.ThreadLocalRandom;

public class ShootingSystem extends IteratingSystem implements Listener<GameEvent> {
    private ComponentMapper<BodyComponent> bodyMapper;
    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<PlayerInventoryComponent> playerDateMapper;
    private ComponentMapper<PlayerStatsComponent> playerStatsMapper;
    private ComponentMapper<SkeletonComponent> skeletonMapper;

    private World world;
    private SkeletonComponent skeletonComponent;
    private PlayerComponent playerComponent;
    private BodyComponent bodyComponent;
    private PlayerInventoryComponent playerInventoryComponent;
    private PlayerStatsComponent statsComponent;
    private RayCastCallback noiseRayCallback;
    private Batch batch;
    private Body body;
    private WeaponMagazine weaponMagazine;
    private RayCastCallback callback;
    private boolean isWeaponShooting;
    private Entity targetEntity;
    private PointLight weaponLight;
    private int direction = 1;
    private float weaponReloadTimer = 0;
    private boolean isMeleeAttackFinish = true;
    private Signal<GameEvent> signal;
    private Vector2 raycastEnd;
    private Array<Entity> noiseTestEntityList;
    private Vector2 vector2temp = new Vector2();
    private BitmapFont font;
    private Camera camera;
    private OrthographicCamera worldCamera;
    private Viewport viewport;

    public ShootingSystem(World world, RayHandler rayHandler, Batch batch, OrthographicCamera camera, Viewport viewport) {
        super(Family.all(PlayerComponent.class).get(), Constants.SHOOTING_SYSTEM);
        this.world = world;
        this.batch = batch;
        this.worldCamera = camera;
        this.viewport = viewport;

        weaponMagazine = new WeaponMagazine();
        bodyMapper = ComponentMapper.getFor(BodyComponent.class);
        playerMapper = ComponentMapper.getFor(PlayerComponent.class);
        playerDateMapper = ComponentMapper.getFor(PlayerInventoryComponent.class);
        playerStatsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);
        skeletonMapper = ComponentMapper.getFor(SkeletonComponent.class);
        raycastEnd = new Vector2();

        weaponLight = new PointLight(rayHandler, 45, new Color(1, 0.8f, 0, 1), 0.8f, 0, 0);
        weaponLight.setStaticLight(false);
        Filter filter = new Filter();
        filter.categoryBits = CollisionID.LIGHT;
        filter.maskBits = CollisionID.MASK_LIGHT;
        weaponLight.setContactFilter(filter);
        weaponLight.setSoftnessLength(0.3f);
        weaponLight.setSoft(true);
        weaponLight.setActive(false);

        signal = new Signal<>();
        noiseTestEntityList = new Array<>();
        noiseRayCallback = (fixture, point, normal, fraction) -> {
            if (fixture.getBody().getUserData() instanceof Entity && ((Entity) fixture.getBody().getUserData()).getComponent(AiComponent.class) != null) {
                noiseTestEntityList.add((Entity) fixture.getBody().getUserData());
            }
            return 1;
        };

        callback = (fixture, point, normal, fraction) -> {
            if (fixture.getBody().getUserData() instanceof Entity && ((Entity) fixture.getBody().getUserData()).getComponent(AiComponent.class) != null) {
                targetEntity = (Entity) fixture.getBody().getUserData();
                return 0;
            } else targetEntity = null;
            return 1;
        };
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        camera = this.getEngine().getSystem(UserInterfaceSystem.class).getStage().getCamera();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        playerComponent = playerMapper.get(entity);
        playerInventoryComponent = playerDateMapper.get(entity);
        statsComponent = playerStatsMapper.get(entity);
        bodyComponent = bodyMapper.get(entity);
        body = bodyMapper.get(entity).body;
        skeletonComponent = skeletonMapper.get(entity);
        weaponReloadTimer += deltaTime;

        Bone root = skeletonComponent.skeleton.getRootBone();
        vector2temp.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(vector2temp);

        Bone bodyTarget = skeletonComponent.skeleton.findBone("armTarget");
        vector2temp.set(root.worldToLocal(vector2temp));
        bodyTarget.setPosition(vector2temp.x, vector2temp.y);

        if (playerComponent.isWeaponHidden) {
            Annihilation.setArrowCursor();
            skeletonComponent.skeleton.findSlot("weapon_pistol").setAttachment(null);
            skeletonComponent.skeleton.findSlot("weapon_rifle").setAttachment(null);
        } else {
            Annihilation.setWeaponCursor();
            if(playerComponent.activeWeapon.getItemId().equals("stg")){
               skeletonComponent.setSkeletonAnimation(false,"weapon_stg_hold",1,false);
            }
            Bone rArmTarget = skeletonComponent.skeleton.findBone("r_hand_target");
            Bone lArmTarget = skeletonComponent.skeleton.findBone("l_hand_target");
            Bone muzzle = skeletonComponent.skeleton.findBone("muzzle");
            Bone grip = skeletonComponent.skeleton.findBone("grip");
            rArmTarget.setPosition(vector2temp.x, vector2temp.y);
            vector2temp.set(grip.getWorldX(), grip.getWorldY());
            vector2temp.set(root.worldToLocal(vector2temp));
            lArmTarget.setPosition(vector2temp.x, vector2temp.y);
        }

        skeletonComponent.skeleton.updateWorldTransform();


//        if (!playerComponent.isWeaponHidden) {
//            world.rayCast(callback, body.getPosition(), raycastEnd.set(body.getPosition().x + 15 * direction, body.getPosition().y));
//            if (targetEntity != null) {
//                BodyComponent targetBody = targetEntity.getComponent(BodyComponent.class);
//                Vector3 worldPosition = worldCamera.project(new Vector3(targetBody.body.getPosition().x, targetBody.body.getPosition().y, 0));
//                batch.setProjectionMatrix(camera.combined);
//                batch.begin();
//                //show accuracy on target
//                font.draw(batch, Math.round(calculateAttackAccuracyFloat() * 100) + "%", worldPosition.x + 45, worldPosition.y + 50);
//                batch.end();
//            }
//        }
    }

    @Override
    public void receive(Signal<GameEvent> signal, GameEvent event) {
        switch (event) {
            case ACTION_BUTTON_TOUCH_DOWN:
                if (!playerComponent.isWeaponHidden) {
                    if(playerComponent.activeWeapon.getCategory() == ItemType.MELEE){
                        meleeAttack();
                    }
                    if(playerComponent.activeWeapon.getCategory() == ItemType.GUNS){
                        startShooting();
                    }
                }
                break;
            case ACTION_BUTTON_TOUCH_UP:
                stopShooting();
                break;
            case WEAPON_TAKE_OUT:
                playerComponent.isWeaponHidden = !playerComponent.isWeaponHidden;
                skeletonComponent.animationState.clearTracks();
                skeletonComponent.skeleton.setToSetupPose();
                break;
            case WEAPON_RELOAD:
                break;
        }
    }

    private void weaponReload(){
        Item weapon = playerComponent.activeWeapon;
        Array<Item> playerInventory = ((EntityEngine) getEngine()).getPlayerInventory();
        for(Item item: playerInventory){
            if(item.getItemType() == weapon.getAmmoType()){
                //to do 
            }
        }

    }


    private void startShooting() {
        if (playerComponent.activeWeapon.isAutomatic()) {
            isWeaponShooting = true;
            automaticWeaponShoot();
        } else if (weaponReloadTimer > playerComponent.activeWeapon.getReloadTime()) {
            weaponShoot();
            weaponReloadTimer = 0;
        }
    }

    private void stopShooting(){
        isWeaponShooting = false;
    }

    private void automaticWeaponShoot() {
        com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
            @Override
            public void run() {
                if (isWeaponShooting) {
                    weaponShoot();
                } else {
                    this.cancel();
                }
            }
        }, 0, playerComponent.activeWeapon.getReloadTime());
    }

    private void weaponShoot() {
        Item weapon = playerComponent.activeWeapon;
        if (weapon.getAmmoInClip() > 0) {
            world.rayCast(callback, body.getPosition(), raycastEnd.set(body.getPosition().x + 15 * direction, body.getPosition().y));
            if (calculateAttackAccuracy() && targetEntity != null) {
                System.out.println("hit");
                targetEntity.getComponent(HealthComponent.class).hp -= playerComponent.activeWeapon.getDamage();
                targetEntity.getComponent(HealthComponent.class).isHit = true;
                targetEntity.getComponent(HealthComponent.class).attackerPosition = bodyComponent.body.getPosition();
            }
            targetEntity = null;
            shootingLight();
            simulatingGunShootNoise();
            createShellAndBullet();
            Sound sound = Annihilation.getAssets().get("sfx/weapons/cg1.wav");
            sound.play();
            weapon.setAmmoInClip(weapon.getAmmoInClip()-1);
        } else {
            Sound sound = Annihilation.getAssets().get("sfx/weapons/no_ammo.wav");
            sound.play();
            stopShooting();
        }
    }

//    private void weaponShoot() {
//        if (weaponMagazine.hasAmmo()) {
//            world.rayCast(callback, body.getPosition(), raycastEnd.set(body.getPosition().x + 15 * direction, body.getPosition().y));
//            if (calculateAttackAccuracy() && targetEntity != null) {
//                System.out.println("hit");
//                targetEntity.getComponent(HealthComponent.class).hp -= playerComponent.activeWeapon.getDamage();
//                targetEntity.getComponent(HealthComponent.class).isHit = true;
//                targetEntity.getComponent(HealthComponent.class).attackerPosition = bodyComponent.body.getPosition();
//            }
//            targetEntity = null;
//            shootingLight();
//            simulatingGunShootNoise();
//            createShellAndBullet();
//            Sound sound = Annihilation.getAssets().get("sfx/cg1.wav");
//            sound.play();
//            weaponMagazine.removeAmmoFromMagazine();
//        } else {
//            weaponMagazine.reload();
//        }
//    }


    private float calculateAttackAccuracyFloat() {
        float weaponAccuracy = playerComponent.activeWeapon.getAccuracy();
        float playerSkill = 0;
        ItemType weaponType = playerComponent.activeWeapon.getItemType();

        switch (weaponType) {
            case WEAPON_MELEE:
                playerSkill = statsComponent.meleeWeapons;
                break;
            case WEAPON_ENERGETIC:
                playerSkill = statsComponent.energeticWeapons;
                break;
            case WEAPON_SHORT:
            case WEAPON_LONG:
                playerSkill = statsComponent.ballisticWeapons;
                break;
        }
        float playerAccuracy = ((float) playerSkill * 0.005f + weaponAccuracy);
        if (playerAccuracy >= 0.95f) {
            return 0.95f;
        } else {
            float distance = targetEntity.getComponent(BodyComponent.class).body.getPosition().x - body.getPosition().x;
            if (distance > 0) {
                distance = distance * -1;
            }
            distance = distance / 100;
            return playerAccuracy + distance;
        }
    }

    private void meleeAttack() {
        if (!playerComponent.isWeaponHidden & isMeleeAttackFinish) {
//            isMeleeAttackFinish = false;
//            animationComponent.isAnimationFinish = false;
//            animationComponent.time = 0;
//            world.rayCast(callback, body.getPosition(), new Vector2(body.getPosition().x + direction, body.getPosition().y));
//            animationComponent.animationState = AnimationStates.MELEE;
//            float animationTimer = animationComponent.animationMap.get(AnimationStates.MELEE.toString()).getAnimationDuration();
//            playerComponent.canMoveOnSide = false;
//
//            Timer.schedule(new Timer.Task() {
//                @Override
//                public void run() {
//                    world.rayCast(callback, body.getPosition(), new Vector2(body.getPosition().x + direction, body.getPosition().y));
//                    if (calculateAttackAccuracy() && targetEntity != null) {
//                        targetEntity.getComponent(HealthComponent.class).hp -= playerComponent.activeWeapon.getDamage();
//                    }
//                    animationComponent.isAnimationFinish = true;
//                    playerComponent.canMoveOnSide = true;
//                    isMeleeAttackFinish = true;
//                }
//            }, animationTimer);
        }
    }



    private void semiAutomaticShoot() {
        if (weaponReloadTimer > playerComponent.activeWeapon.getReloadTime()) {
            weaponShoot();
            weaponReloadTimer = 0;
        }
    }



    private void simulatingGunShootNoise() {
        world.rayCast(noiseRayCallback, body.getPosition().x, body.getPosition().y,
                body.getPosition().x + 12, body.getPosition().y);

        world.rayCast(noiseRayCallback, body.getPosition().x, body.getPosition().y,
                body.getPosition().x - 12, body.getPosition().y);
        for (Entity entity : noiseTestEntityList) {
            AnimationComponent animationComponentAi = entity.getComponent(AnimationComponent.class);
            AiComponent aiComponent = entity.getComponent(AiComponent.class);
            aiComponent.isHearEnemy = true;
            aiComponent.enemyPosition = body.getPosition();
        }
        noiseTestEntityList.clear();
    }


    private void createShellAndBullet() {

//
//        Vector3 worldPosition = worldCamera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
//        float x = body.getPosition().x;
//        float y = body.getPosition().y;
//        float targetX = worldPosition.x;
//        float targetY = worldPosition.y;
//
//        float muzzleX = skeletonComponent.skeleton.findBone("muzzle").getWorldX();
//        float muzzleY = skeletonComponent.skeleton.findBone("muzzle").getWorldY();
//        this.getEngine().addEntity(EntityFactory.getInstance().createBulletShellEntity(muzzleX, muzzleY));
//        this.getEngine().addEntity(EntityFactory.getInstance().createBulletEntity(muzzleX, muzzleY, targetX, targetY, 30, animationComponent.spriteDirection));
//        this.getEngine().addEntity(EntityFactory.getInstance().createShootSplashEntity(muzzleX, muzzleY, animationComponent.spriteDirection));
    }




    /**
     * true = hit, false = miss
     */
    private boolean calculateAttackAccuracy() {
        float weaponAccuracy = playerComponent.activeWeapon.getAccuracy();
        float playerSkill = 0;
//        int weaponType = playerComponent.activeWeapon.getItemUseType();
//
//        switch (weaponType) {
//            case 4:
//                playerSkill = statsComponent.meleeWeapons;
//                break;
//            case 8:
//                playerSkill = statsComponent.energeticWeapons;
//                break;
//            case 16:
//                playerSkill = statsComponent.energeticWeapons;
//                break;
//            case 32:
//                playerSkill = statsComponent.ballisticWeapons;
//                break;
//            case 64:
//                playerSkill = statsComponent.ballisticWeapons;
//                break;
//        }
        float playerAccuracy = ((float) playerSkill * 0.005f + weaponAccuracy);
        if (playerAccuracy >= 0.95f) {
            return true;
        } else {
            double randomBonus = ThreadLocalRandom.current().nextDouble(playerAccuracy, 1);
//            float randomBonus =  randomGenerator.nextFloat() * (0.99f - playerAccuracy) + playerAccuracy;
            if (randomBonus >= 0.95f) {
                System.out.println("Player accuracy + bonus: " + randomBonus);
                return true;
            }
        }
        System.out.println("miss ");
        return false;
    }

    private int calcualteAttackDamage() {
        //TODO
        return 0;
    }


    private int addAmmoFromInventory() {
        int ammoInInventory = 0;
//        for (InventoryItemLocation item : playerInventoryComponent.inventoryItem) {
//            if (item.getItemID().equals(playerComponent.activeWeapon.getAmmoID().toString())) {
//                ammoInInventory = item.getItemsAmount();
//                if (ammoInInventory < playerComponent.activeWeapon.getMaxAmmoInMagazine()) {
//                    playerInventoryComponent.inventoryItem.removeValue(item, false);
//                    return ammoInInventory;
//                } else {
//                    item.setItemsAmount(item.getItemsAmount() - playerComponent.activeWeapon.getMaxAmmoInMagazine());
//                    return playerComponent.activeWeapon.getMaxAmmoInMagazine();
//                }
//            }
//        }
        return ammoInInventory;
    }


    private void shootingLight() {
//        switch (playerComponent.activeWeapon.getItemID()) {
//            case P38:
//                weaponLight.attachToBody(body, direction - 0.1f, 0.5f);
//                break;
//            case MP44:
//                weaponLight.attachToBody(body, direction - 0.1f, 0.3f);
//                break;
//        }

        weaponLight.setActive(true);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                weaponLight.setActive(false);
            }
        }, 0.1f);
    }

    void playAnimation(Animation animation, float animationTime) {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                weaponLight.setActive(false);
            }
        }, animationTime);
    }


    private class WeaponMagazine {

        private int ammoInMagazine;
        private int maxAmmoInMagazine;

        private WeaponMagazine() {

        }

        void reload() {
            setAmmoInMagazine(addAmmoFromInventory());
        }

        boolean hasAmmo() {
            if (ammoInMagazine > 0) {
                return true;
            }
            return false;
        }

        int getAmmoInMagazine() {

            return ammoInMagazine;
        }

        void setAmmoInMagazine(int ammoInMagazine) {
            this.ammoInMagazine = ammoInMagazine;
        }

        void removeAmmoFromMagazine() {
            ammoInMagazine--;
        }

        void setMaxAmmoInMagazine(int maxAmmoInMagazine) {
            this.maxAmmoInMagazine = maxAmmoInMagazine;
        }

    }
}


