package com.cosma.annihilation.Editor.CosmaMap;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.cosma.annihilation.Annihilation;
import com.cosma.annihilation.Utils.Constants;

public class Sprite implements Comparable<Sprite>{
    private float x, y, width, height, angle;
    private String atlasRegionName;
    private TextureRegion textureRegion;
    private Texture normalTexture;
    private boolean renderOnFirstPlane = false;
    private int order = 5;
    private boolean isHighlighted = false;


    public Sprite() {
    }

    void setTextureDate(String region, String path) {
        this.textureRegion = Annihilation.getAssets().get(path, TextureAtlas.class).findRegion(region);
        this.atlasRegionName = region;
        this.width = textureRegion.getRegionWidth() / Constants.PPM;
        this.height = textureRegion.getRegionHeight() / Constants.PPM;
        String normalMapPath = ((FileTextureData) textureRegion.getTexture().getTextureData()).getFileHandle().pathWithoutExtension();
        if (Annihilation.getAssets().isLoaded(normalMapPath + "_n.png")) {
            this.normalTexture = Annihilation.getAssets().get(normalMapPath + "_n.png");
            this.normalTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } else {
            System.out.println("normal map not found!");
        }
    }

    public void setSpritePosition(float x, float y, float angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void setSpritePosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setSpriteAngle(float angle) {
        this.angle = angle;
    }

    String getAtlasPath() {
        return ((FileTextureData) textureRegion.getTexture().getTextureData()).getFileHandle().pathWithoutExtension() + ".atlas" + "," + atlasRegionName;
    }

    public TextureRegion getTextureRegion() {
        return textureRegion;
    }

    public void bindNormalTexture(int tex) {
        if (normalTexture != null) {
            normalTexture.bind(tex);
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getAngle() {
        return angle;
    }

    public int getRenderOrder() {
        return order;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }

    public void setRenderOrder(int order) {
        this.order = order;
    }

    public boolean isRenderOnFirstPlane() {
        return renderOnFirstPlane;
    }

    public void setRenderOnFirstPlane(boolean renderOnFirstPlane) {
        this.renderOnFirstPlane = renderOnFirstPlane;
    }

    private boolean flipX = false, flipY = false;

    public boolean isFlipX() {
        return flipX;
    }

    public boolean isFlipY() {
        return flipY;
    }

    @Override
    public int compareTo(Sprite o) {
        return Integer.compare(order, o.order);
    }

    public Texture getNormalTexture() {
        return normalTexture;
    }
}
