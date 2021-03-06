package com.cosma.annihilation.Systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cosma.annihilation.Annihilation;
import com.cosma.annihilation.Box2dLight.PointLight;
import com.cosma.annihilation.Box2dLight.RayHandler;
import com.cosma.annihilation.Components.*;
import com.cosma.annihilation.EntityEngine.core.ComponentMapper;
import com.cosma.annihilation.EntityEngine.core.Entity;
import com.cosma.annihilation.EntityEngine.core.Family;
import com.cosma.annihilation.EntityEngine.signals.Listener;
import com.cosma.annihilation.EntityEngine.signals.Signal;
import com.cosma.annihilation.EntityEngine.systems.IteratingSystem;
import com.cosma.annihilation.EntityEngine.utils.ImmutableArray;
import com.cosma.annihilation.Items.Item;
import com.cosma.annihilation.Items.ItemType;
import com.cosma.annihilation.Utils.Constants;
import com.cosma.annihilation.Utils.CollisionID;
import com.cosma.annihilation.Utils.Enums.GameEvent;
import com.cosma.annihilation.Utils.ShootEngine;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.attachments.RegionAttachment;

import java.util.concurrent.ThreadLocalRandom;

public class ShootingSystem extends IteratingSystem implements Listener<GameEvent> {
    private ComponentMapper<PhysicsComponent> bodyMapper;
    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<PlayerInventoryComponent> playerDateMapper;
    private ComponentMapper<PlayerStatsComponent> playerStatsMapper;
    private ComponentMapper<SkeletonComponent> skeletonMapper;

    private World world;
    private SkeletonComponent skeletonComponent;
    private PlayerComponent playerComponent;
    private PhysicsComponent physicsComponent;
    private PlayerInventoryComponent playerInventoryComponent;
    private PlayerStatsComponent statsComponent;
    private RayCastCallback noiseRayCallback;
    private Batch batch;
    private Body body;
    private RayCastCallback attackCallback;
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
    private OrthographicCamera worldCamera;
    private Viewport viewport;
    private int meleeBlowNumber = 1;
    private RayHandler rayHandler;
    private int weaponSpread = 0;
    private boolean isMouseButtonPressed = false;
    private ShootEngine shootEngine;

    public ShootingSystem(World world, RayHandler rayHandler, Batch batch, OrthographicCamera camera, Viewport viewport) {
        super(Family.all(PlayerComponent.class).get(), Constants.SHOOTING_SYSTEM);
        this.world = world;
        this.batch = batch;
        this.worldCamera = camera;
        this.viewport = viewport;
        this.rayHandler = rayHandler;


        bodyMapper = ComponentMapper.getFor(PhysicsComponent.class);
        playerMapper = ComponentMapper.getFor(PlayerComponent.class);
        playerDateMapper = ComponentMapper.getFor(PlayerInventoryComponent.class);
        playerStatsMapper = ComponentMapper.getFor(PlayerStatsComponent.class);
        skeletonMapper = ComponentMapper.getFor(SkeletonComponent.class);
        raycastEnd = new Vector2();

        weaponLight = new PointLight(rayHandler, 45, new Color(1, 1f, 0.4f, 0.7f), 4f, 0, 0);
        weaponLight.setStaticLight(false);

        Filter filter = new Filter();
        filter.categoryBits = CollisionID.LIGHT;
        filter.maskBits = CollisionID.MASK_LIGHT;
        weaponLight.setContactFilter(filter);
        weaponLight.setXray(true);
        weaponLight.setActive(false);

        signal = new Signal<>();
        noiseTestEntityList = new Array<>();
        noiseRayCallback = (fixture, point, normal, fraction) -> {
            if (fixture.getBody().getUserData() instanceof Entity && ((Entity) fixture.getBody().getUserData()).getComponent(AiComponent.class) != null) {
                noiseTestEntityList.add((Entity) fixture.getBody().getUserData());
            }
            return 1;
        };

        attackCallback = (fixture, point, normal, fraction) -> {
            if (fixture.getBody().getUserData() instanceof Entity && ((Entity) fixture.getBody().getUserData()).getComponent(AiComponent.class) != null) {
                targetEntity = (Entity) fixture.getBody().getUserData();
                return 0;
            } else targetEntity = null;
            return 1;
        };
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        playerComponent = playerMapper.get(entity);
        playerInventoryComponent = playerDateMapper.get(entity);
        statsComponent = playerStatsMapper.get(entity);
        physicsComponent = bodyMapper.get(entity);
        body = bodyMapper.get(entity).body;
        skeletonComponent = skeletonMapper.get(entity);
        weaponReloadTimer += deltaTime;

        Bone root = skeletonComponent.skeleton.getRootBone();
        vector2temp.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(vector2temp);

        Bone bodyTarget = skeletonComponent.skeleton.findBone("bodyTarget");
        vector2temp.set(root.worldToLocal(vector2temp));
        bodyTarget.setPosition(vector2temp.x, vector2temp.y);

        if (skeletonComponent.skeletonDirection) {
            direction = 1;
        } else {
            direction = -1;
        }

        if (playerComponent.isWeaponHidden) {
            Annihilation.setArrowCursor();
            skeletonComponent.skeleton.findSlot("weapon_pistol").setAttachment(null);
            skeletonComponent.skeleton.findSlot("weapon_rifle").setAttachment(null);
        } else if (playerComponent.canShoot) {
            Annihilation.setWeaponCursor();
            skeletonComponent.setSkeletonAnimation(false, playerComponent.activeWeapon.getHoldAnimation(), 2, true);
            Bone rArmTarget = skeletonComponent.skeleton.findBone("r_hand_target");
            Bone lArmTarget = skeletonComponent.skeleton.findBone("l_hand_target");
            Bone flash = skeletonComponent.skeleton.findBone("flash");
            Bone grip = skeletonComponent.skeleton.findBone("grip");
            rArmTarget.setPosition(vector2temp.x, vector2temp.y);
            vector2temp.set(grip.getWorldX(), grip.getWorldY());
            vector2temp.set(root.worldToLocal(vector2temp));
            lArmTarget.setPosition(vector2temp.x, vector2temp.y);
        }

        if(isMouseButtonPressed){

        }


        skeletonComponent.skeleton.updateWorldTransform();



//        if (!playerComponent.isWeaponHidden) {
//            world.rayCast(callback, body.getPosition(), raycastEnd.set(body.getPosition().x + 15 * direction, body.getPosition().y));
//            if (targetEntity != null) {
//                PhysicsComponent targetBody = targetEntity.getComponent(PhysicsComponent.class);
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
                isMouseButtonPressed = true;
                if (!playerComponent.isWeaponHidden) {
                    if (playerComponent.activeWeapon.getCategory() == ItemType.MELEE) {
                        meleeAttack();
                    }
                    if (playerComponent.activeWeapon.getCategory() == ItemType.GUNS && playerComponent.canShoot) {
                        startShooting();
                    }
                }
                break;
            case ACTION_BUTTON_TOUCH_UP:
                isMouseButtonPressed = false;
                stopShooting();
                break;
            case WEAPON_TAKE_OUT:
                playerComponent.isWeaponHidden = !playerComponent.isWeaponHidden;
                skeletonComponent.animationState.addEmptyAnimation(2, 0.3f, 0);
                skeletonComponent.skeleton.setToSetupPose();
                break;
            case WEAPON_RELOAD:
                weaponReload();
                break;
        }
    }

    private void weaponReload() {
        playerComponent.canShoot = false;
        Item weapon = playerComponent.activeWeapon;
        boolean removeItem = false;
        Item itemToRemove = null;
        Array<Item> playerInventory = getEngine().getPlayerInventory();
        for (Item item : playerInventory) {
            if (item.getItemType() == weapon.getAmmoType() && playerComponent.canShoot) {
                if (item.getItemAmount() - weapon.getMaxAmmoInClip() > 0) {
                    item.setItemAmount(item.getItemAmount() - weapon.getMaxAmmoInClip());
                    weapon.setAmmoInClip(weapon.getMaxAmmoInClip());
                } else {
                    int count = item.getItemAmount() - weapon.getMaxAmmoInClip();
                    count = weapon.getMaxAmmoInClip() + count;
                    weapon.setAmmoInClip(count);
                    itemToRemove = item;
                    removeItem = true;
                }
                playerComponent.canShoot = false;
                weaponReloadAnimationPlay();
                if (removeItem) {
                    playerInventory.removeValue(itemToRemove, true);
                }
            }
        }
    }

    private void weaponReloadAnimationPlay() {
        skeletonComponent.setSkeletonAnimation(false, playerComponent.activeWeapon.getReloadAnimation(), 4, false);
        playerComponent.canShoot = false;
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                playerComponent.canShoot = true;
                skeletonComponent.animationState.setEmptyAnimation(4, 0.2f);
            }
        }, skeletonComponent.animationState.getCurrent(4).getAnimation().getDuration());
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

    private void stopShooting() {
        isWeaponShooting = false;
    }

    private void automaticWeaponShoot() {
        com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
            @Override
            public void run() {
                if (isWeaponShooting) {
                    weaponShoot();
                    /// TODO: 25.10.2020
                    weaponSpread -= 0;
                    Gdx.input.setCursorPosition(Gdx.input.getX(), Gdx.input.getY() + weaponSpread);
                } else {
                    weaponSpread = 0;
                    this.cancel();
                }
            }
        }, 0, playerComponent.activeWeapon.getReloadTime());
    }


    private void weaponShoot() {
        Item weapon = playerComponent.activeWeapon;

//
//                ((RegionAttachment) skeletonComponent.skeleton.findSlot("weapon_rifle").getAttachment())
//                        .setRegion(skeletonComponent.diffuseTextureAtlas.findRegion("weapon_stg_scope"));


        if (weapon.getAmmoInClip() > 0) {
            vector2temp.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(vector2temp);
            spawnBulletHole(vector2temp.x, vector2temp.y);


//             Gdx.input.setCursorPosition(Gdx.input.getX(), MathUtils.round(Gdx.input.getY()-(Gdx.graphics.getHeight()*weapon.getWeaponRecoil())));
//            Bone armTarget = skeletonComponent.skeleton.findBone("r_hand_target");
////            armTarget.setWorldY(armTarget.getWorldY()+1*weapon.getWeaponRecoil());
//            armTarget.setPosition(armTarget.getX(),armTarget.getY()+1);
////            armTarget.setWorldY(armTarget.getWorldY()+1);
            skeletonComponent.skeleton.updateWorldTransform();
            skeletonComponent.setSkeletonAnimation(false, playerComponent.activeWeapon.getShootAnimation(), 4, false);
            skeletonComponent.animationState.addEmptyAnimation(4, 0.1f, 0.1f);
            world.rayCast(attackCallback, body.getPosition(), raycastEnd.set(body.getPosition().x + 15 * direction, body.getPosition().y));
            if (calculateAttackAccuracy() && targetEntity != null) {
                targetEntity.getComponent(HealthComponent.class).hp -= playerComponent.activeWeapon.getDamage();
                targetEntity.getComponent(HealthComponent.class).isHit = true;
                targetEntity.getComponent(HealthComponent.class).attackerPosition = physicsComponent.body.getPosition();
            }
            targetEntity = null;
            createBulletAndLightEffect();
            simulatingGunShootNoise();
            Sound sound = Annihilation.getAssets().get("sfx/weapons/cg1.wav");
            sound.play();
            weapon.setAmmoInClip(weapon.getAmmoInClip() - 1);
        } else {
            Sound sound = Annihilation.getAssets().get("sfx/weapons/no_ammo.wav");
            sound.play();
            stopShooting();
        }
    }

    private void createBulletAndLightEffect() {
        Bone muzzle = skeletonComponent.skeleton.findBone("muzzle");
        Bone target = skeletonComponent.skeleton.findBone("target");
        Bone shellEjector = skeletonComponent.skeleton.findBone("shell_ejector");
        float angle = vector2temp.set(target.getWorldX(), target.getWorldY()).sub(muzzle.getWorldX(), muzzle.getWorldY()).angle();
//        getEngine().spawnBulletEntity(muzzle.getWorldX(), muzzle.getWorldY(), angle, 25, skeletonComponent.skeletonDirection);
//        this.getEngine().addEntity(EntityFactory.getInstance().createBulletShellEntity(shellEjector.getWorldX(), shellEjector.getWorldY()));
//        this.getEngine().addEntity(EntityFactory.getInstance().createBulletEntity(muzzleX, muzzleY, targetX, targetY, 30, animationComponent.spriteDirection));

        weaponLight.setActive(true);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                weaponLight.setActive(false);
            }
        }, 0.2f);
    }

    private boolean calculateAttackAccuracyFloat() {
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
            return true;
        } else {
            float distance = targetEntity.getComponent(PhysicsComponent.class).body.getPosition().x - body.getPosition().x;
            if (distance > 0) {
                distance = distance * -1;
            }
            distance = distance / 100;
            return true;
        }
    }

    private void meleeAttack() {
        if (isMeleeAttackFinish) {
            isMeleeAttackFinish = false;
            playerComponent.canMoveOnSide = false;
            skeletonComponent.skeleton.updateWorldTransform();
            switch (meleeBlowNumber) {
                case 1:
                    skeletonComponent.setSkeletonAnimation(false, "fist_r_punch", 4, false);
                    meleeBlowNumber++;
                    break;
                case 2:
                    skeletonComponent.setSkeletonAnimation(false, "fist_l_punch", 4, false);
                    meleeBlowNumber--;
                    break;
            }

            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    playerComponent.canMoveOnSide = true;
                    isMeleeAttackFinish = true;
                    if (true) {
                        attackRaycast(true);
                        if (targetEntity != null) {
                            targetEntity.getComponent(HealthComponent.class).decreaseHp(playerComponent.activeWeapon.getDamage());
                            targetEntity.getComponent(HealthComponent.class).hit(body.getPosition());
                        }
                        targetEntity = null;
                    }
                }
            }, skeletonComponent.animationState.getCurrent(4).getAnimation().getDuration());
            skeletonComponent.animationState.addEmptyAnimation(4, 0.2f, skeletonComponent.animationState.getCurrent(4).getAnimation().getDuration());
        }
    }

    /**
     * Set targetEntity to null after every use
     */
    private void attackRaycast(boolean isMelee) {
        if (isMelee) {
            raycastEnd.set(body.getPosition().x + (playerComponent.activeWeapon.getRange() + 0.5f) * direction, body.getPosition().y);
            world.rayCast(attackCallback, body.getPosition(), raycastEnd);
        } else {
            vector2temp.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(vector2temp);
            world.rayCast(attackCallback, body.getPosition(), vector2temp);
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

    void spawnBulletHole(float x, float y) {
        if (this.getEngine().isPointInDrawField(x, y)) {
            Entity entity = new Entity();
            TextureComponent textureComponent = new TextureComponent();
            textureComponent.textureRegion = Annihilation.getTextureRegion("fx_textures", "bullet_hole");
            textureComponent.normalTexture = Annihilation.getAssets().get("gfx/atlas/fx_textures_n.png", Texture.class);
            entity.add(textureComponent);

            DrawOrderComponent drawOrderComponent = new DrawOrderComponent();
            drawOrderComponent.drawOrder = 3;
            entity.add(drawOrderComponent);

            SpriteComponent spriteComponent = new SpriteComponent();
            spriteComponent.x = x;
            spriteComponent.y = y;
            spriteComponent.createRectangle(textureComponent);
            spriteComponent.drawDiffuse = false;
            entity.add(spriteComponent);

            ImmutableArray<Entity> entities = getEngine().getEntitiesFor(Family.all(SpriteComponent.class).get());
            if (entities.size() < 1) {
                getEngine().addEntity(entity);
            } else {
                boolean overlaps = false;
                for (Entity spriteEntity : entities) {
                    if (spriteComponent.rectangle.overlaps(spriteEntity.getComponent(SpriteComponent.class).rectangle)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    getEngine().addEntity(entity);
                }
            }
        }
    }

    void playAnimation(Animation animation, float animationTime) {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                weaponLight.setActive(false);
            }
        }, animationTime);
    }
}


