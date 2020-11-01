package com.cosma.annihilation.Components;

import com.cosma.annihilation.EntityEngine.core.Component;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class TextureComponent implements Component {
    public String texturePatch;
    public String atlasPatch;
    public Texture texture;
    public Texture normalTexture;
    public TextureRegion textureRegion;
    public  boolean renderAfterLight = false;
    public boolean flipTexture = false;
    public boolean direction = false;
    public boolean renderWithShader = false;


}
