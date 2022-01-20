package com.mpaun.game;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.TiledTextureRegion;

public class Key {
	
	Hexagon parent;
	Sprite body;
	
	Key(Hexagon pParent, TiledTextureRegion texture) {
		parent = pParent;
		
		float centerY = parent.centerY;
		float centerX = parent.centerX + parent.topWidth / 2;
		float size = 80;
		
		body = new Sprite(centerX - size / 2, centerY - size / 2, 
				texture, parent.vertexBufferObjectManager);
		body.setColor(0.8f, 0.7f, 0.4f);
		parent.scene.attachChild(body);
	}

}
