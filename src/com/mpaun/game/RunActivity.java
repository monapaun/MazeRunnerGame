package com.mpaun.game;

import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.color.Color;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import android.content.Context;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.widget.Toast;

public class RunActivity extends SimpleBaseGameActivity implements IOnSceneTouchListener  {
	
	// SCENE VARIABLES
		// Dimensions of the camera window.
		// These must be set initially, setting them base don true screen sizes does not work.
		private static final int CAMERA_WIDTH = 2048;
		private static final int CAMERA_HEIGHT = 1178;
		// Camera window / view into world.
		private BoundCamera playerCamera;
		// Model of the world.
		private Scene scene;
		// Physics interaction control of material (static and mobile) objects in the world.
		private PhysicsWorld physicsWorld;
		// vertexBufferObjectManager - Handles art image displaying, by managing GLES2 vertexes.
		private VertexBufferObjectManager vertexBufferObjectManager;
		Context context = this;
		
		// PLAYER VARIABLES
		// Starting coordinates.
		private static final int PLAYER_START_X = 0;
		private static final int PLAYER_START_Y = 50;
		// Look / art.
		private BitmapTextureAtlas bitmapTextureAtlas;
		private TiledTextureRegion playerTextureRegion;
		private AnimatedSprite playerFace;
		// Physics container.
		private Body playerBody;
		// Velocity.
		private float playerVerticalVelocity;
		private float playerHorizontalVelocity = 5f;
		private float jumpVelocity = -12f;
		// Movement constrains.
		private int jumpState = 0;
		private boolean direction = true; 
		int playerCounter = 0;
		
		// BACKGROUND.
		BitmapTextureAtlas backgroundTextureAtlas;
		BitmapTextureAtlas foregroundTextureAtlas;
		TextureRegion backgroundLayer;
		TextureRegion foregroundLayer;
		
		// HUD graphics.
		BitmapTextureAtlas buttonTextureAtlas;
		TiledTextureRegion turnTextureRegion;
		TiledTextureRegion shrinkTextureRegion;
		TiledTextureRegion growTextureRegion;
		TiledTextureRegion jumpTextureRegion;
		TiledTextureRegion anvilTextureRegion;
		
		// Physics characteristics of walls and player.
		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0f, 0f, 0f);
		final FixtureDef playerFixtureDef = PhysicsFactory.createFixtureDef(1f, 0f, 0f);
		
		// Environment constants.
		static int sizePlat = 400;
		Sprite[] platforms = new Sprite[sizePlat];
		Body[] bodies = new Body[sizePlat];		
		private static final int PLATFORM_SIDE = 32;
		TiledTextureRegion platformTextureRegion;
		TiledTextureRegion enemyTextureRegion;
		int newestPlatform = sizePlat - 1 ;
		int platformState = 0;
		int threshDiff = 1000;
		
		// Object collections.
		Sprite[] enemyFace = new Sprite[12];
		Body[] enemyBody = new Body[12];
		Sprite[] anvilFace = new Sprite[12];
		Body[] anvilBody = new Body[12];
		int newestEnemy = 11;
		int newestAnvil = 11;
		
		Font font;
		Text passedTime;
		int secondsPassed = 0;
	
	@Override
	public EngineOptions onCreateEngineOptions() {
		Toast.makeText(this, "Please keep device in landscape mode!", Toast.LENGTH_SHORT).show();
		
		// Camera that will follow the player.
		// 1st, 2nd parameters: initial point in model for the upper-right corner of camera.
		// 3rd, 4th parameters: how much of the model is caught in the camera window.
		// 5th, 6th parameters: model x-axis bounds the camera may display.
		// 7th, 8th parameters: model y-axis bounds the camera may display. 
		this.playerCamera = new BoundCamera(PLAYER_START_X, PLAYER_START_Y, 
				CAMERA_WIDTH, CAMERA_HEIGHT, 0, 0, 0, 0);
		
		// 1st parameter: is Activity full screen?
		// 2nd parameter: Activity orientation.
		// 3rd: describes the ratio of the dimensions of the rectangle in which the camera(s) will go into
		// Maximum possible display-able rectangle will be used, centered on the less used dimension.
		// 4th parameter: first/only Camera object.
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_SENSOR, 
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.playerCamera);
	}
	
	@Override
	protected void onCreateResources() {
		// Load all the graphics used by this Activity.
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("graphics/");
		
		//Player resources.
		// 64 * 32 is the area for all the graphics loaded in this Activity in total.
		this.bitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 128, 320, TextureOptions.BILINEAR);
		// The graphics for the player are two squares, forming a rectangle stored at (0,0) in the Atlas. 
		this.playerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bitmapTextureAtlas, 
				this, "player.png", 0, 0, 2, 1);
		this.anvilTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bitmapTextureAtlas, 
				this, "anvil2.png", 0, 64, 1, 1);
		this.platformTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bitmapTextureAtlas, 
				this, "box.png", 0, 128, 1, 1);
		this.enemyTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bitmapTextureAtlas, 
				this, "enemy2.png", 0, 256, 1, 1);
		this.bitmapTextureAtlas.load();	
		
		// Background resources.
		this.backgroundTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 2048, 1178, TextureOptions.BILINEAR);
		this.backgroundLayer = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.backgroundTextureAtlas, this, "background2.png", 0, 0);
		this.backgroundTextureAtlas.load();
		this.foregroundTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 2048, 1178, TextureOptions.BILINEAR);
		this.foregroundLayer = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.foregroundTextureAtlas, this, "foreground2.png", 0, 0);
		this.foregroundTextureAtlas.load();
		
		// HUD buttons resources.
		this.buttonTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 320, 296, TextureOptions.BILINEAR);
		this.turnTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "return.png", 0, 0, 1, 1);
		this.shrinkTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "shrink.png", 0, 74, 1, 1);
		this.growTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "grow.png", 0, 148, 1, 1);
		this.jumpTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "jump.png", 0, 222, 1, 1);
		this.buttonTextureAtlas.load();
		
		this.font = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, 
				Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 32, Color.WHITE.hashCode());
		this.font.load();
	}
	
	@Override
	protected Scene onCreateScene() {
		// Keeps count of FPS (debugging purposes).
		// this.mEngine.registerUpdateHandler(new FPSLogger());
		
		// All things in the world (material and immaterial) will be bound to this.
		this.scene = new Scene();
		
		// Define physics characteristics of whole world.
		this.physicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);
		this.scene.registerUpdateHandler(this.physicsWorld);
		
		// Background management.
		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
		vertexBufferObjectManager = this.getVertexBufferObjectManager();
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, CAMERA_HEIGHT - this.backgroundLayer.getHeight(), this.backgroundLayer, vertexBufferObjectManager)));
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, 80, this.foregroundLayer, vertexBufferObjectManager)));
		scene.setBackground(autoParallaxBackground);
		
		// Add controllers.
		createControllers();
		
		// Add player to the world.
		playerFace = new AnimatedSprite(PLAYER_START_X, PLAYER_START_Y, 
				this.playerTextureRegion, vertexBufferObjectManager);
		playerFace.animate(100);
		playerBody = PhysicsFactory.createCircleBody(this.physicsWorld, playerFace, BodyType.DynamicBody, playerFixtureDef);
		this.scene.attachChild(playerFace);
		this.physicsWorld.registerPhysicsConnector(new PhysicsConnector(playerFace, playerBody, true, true));
		
		// Let camera chase the player regardless of its location.
		this.playerCamera.setChaseEntity(playerFace);
		this.playerCamera.setBoundsEnabled(false);
		
		// Add initial platforms.
		for (int i = 0; i < platforms.length; i++) {
			platforms[i] = new Sprite((i - sizePlat + 25)* PLATFORM_SIDE, 300,
					this.platformTextureRegion, vertexBufferObjectManager);
			bodies[i] = PhysicsFactory.createBoxBody(this.physicsWorld, platforms[i], 
					BodyType.StaticBody, wallFixtureDef);
			this.scene.attachChild(platforms[i]);
		}
		// Next state.
		double auxRand = Math.random();
		if (auxRand < 0.2)
			platformState = 1;
		else if (auxRand < 0.4)
			platformState = 2;
		else if (auxRand < 0.6)
			platformState = 3;
		else if (auxRand < 0.8)
			platformState = 4;
		else
			platformState = 5;
				
		// Check if player returned to ground.
		// Use this approach instead of registering a IUpdateHangler() with an overridden 'onUpdate' function.
		this.scene.registerUpdateHandler(new TimerHandler(0.1f, true, new ITimerCallback(){
			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				// Check if vertical velocity has changed form falling to going up.
				if (playerVerticalVelocity > 0 && playerBody.getLinearVelocity().y <= 0)
					jumpState = 0;
				playerVerticalVelocity = playerBody.getLinearVelocity().y;
				// Check for the player not to have lost all speed.
				// Maintain player speed.
				if (direction)
					playerBody.setLinearVelocity(new Vector2(playerHorizontalVelocity,
							playerBody.getLinearVelocity().y));
				else
					playerBody.setLinearVelocity(new Vector2(-playerHorizontalVelocity,
							playerBody.getLinearVelocity().y));
				// Check if player needs to get to regular size.
				if (playerCounter > 1)
					playerCounter--;
				else if (playerCounter < -1)
					playerCounter++;
				else if (playerCounter == 1) {
					playerCounter = 0;
					playerFace.setScale(1f);
	            	physicsWorld.destroyBody(playerBody);
	            	playerBody = PhysicsFactory.createCircleBody(physicsWorld, playerFace, BodyType.DynamicBody, playerFixtureDef);
					playerHorizontalVelocity *= 1.5;
				} else if (playerCounter == -1) {
					playerCounter = 0;
					playerFace.setScale(1f);
	            	physicsWorld.destroyBody(playerBody);
	            	playerBody = PhysicsFactory.createCircleBody(physicsWorld, playerFace, BodyType.DynamicBody, playerFixtureDef);
					playerHorizontalVelocity /= 1.5;
				}
				// Check if player if falling.
				if (32 * playerBody.getPosition().y - platforms[newestPlatform].getY() > 1000) {
					finish();
				}
			}
		}));
		
		// Update text.
		this.scene.registerUpdateHandler(new TimerHandler(1f, true, new ITimerCallback(){
			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				// Update time.
				secondsPassed++;
				passedTime.setText("Time passed: " + secondsPassed + "s");
			}
		}));
			
		
		// Create new ground.
		// Destroy oldest brick and make a new one to the right based on camera position.
		this.scene.registerUpdateHandler(new TimerHandler(0.2f, true, new ITimerCallback() {
			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				if (platforms[newestPlatform].getX() - 32 * playerBody.getPosition().x < threshDiff) {
					if (platformState == 0) {
						for (int i = 1; i <= 32; i++) {
							// Make note of rightmost platform edge.
							float auxX = platforms[newestPlatform].getX() + PLATFORM_SIDE;
							float auxY = platforms[newestPlatform].getY();
							// Identify platform container to update.
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							// Detach old object.
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							// Create and attach new platform.
							platforms[newestPlatform] = new Sprite(auxX, auxY,
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, platforms[newestPlatform], 
									BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						double auxRand = Math.random();
						if (auxRand < 0.2)
							platformState = 1;
						else if (auxRand < 0.4)
							platformState = 2;
						else if (auxRand < 0.6)
							platformState = 3;
						else if (auxRand < 0.8)
							platformState = 4;
						else
							platformState = 5;
						// Create enemy.
						createEnemy(platforms[newestPlatform].getX() - 32 * 16, platforms[newestPlatform].getY() - 32 * 4);
					} else if (platformState == 1) {
						for (int i = 1; i <= 12; i++) {
							// Make note of rightmost platform edge.
							float auxX = platforms[newestPlatform].getX() + PLATFORM_SIDE;
							float auxY = platforms[newestPlatform].getY();
							// Identify platform container to update.
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							// Detach old object.
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							// Create and attach new platform.
							platforms[newestPlatform] = new Sprite(auxX, (float) (auxY - 16),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						platformState = 0;
					} else if (platformState == 2) {
						for (int i = 1; i <= 12; i++) {
							// Make note of rightmost platform edge.
							float auxX = platforms[newestPlatform].getX() + PLATFORM_SIDE;
							float auxY = platforms[newestPlatform].getY();
							// Identify platform container to update.
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							// Detach old object.
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							// Create and attach new platform.
							platforms[newestPlatform] = new Sprite(auxX, (float) (auxY + 16),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						platformState = 0;
					} else if (platformState == 3) {
						// Three paths - continue upper.
						float firstX = platforms[newestPlatform].getX() + PLATFORM_SIDE;
						float firstY = platforms[newestPlatform].getY();
						// Down slope.
						for (int i = 1; i <= 10; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i - 1) * 32, (float) (firstY + i * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Down horizontal.
						for (int i = 1; i <= 38; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 9) * 32, (float) (firstY + 11 * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Middle horizontal.
						for (int i = 1; i <= 40; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 7) * 32, (float) (firstY),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper slope.
						for (int i = 1; i <= 8; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 23) * 32, (float) (firstY - (i + 4) * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper horizontal.
						for (int i = 1; i <= 16; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 31) * 32, (float) (firstY - 12 * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}		
						// Change the land type.
						platformState = 0;
					} else if (platformState == 4) {
						// Three paths - continue middle.
						float firstX = platforms[newestPlatform].getX() + PLATFORM_SIDE;
						float firstY = platforms[newestPlatform].getY();
						// Down slope.
						for (int i = 1; i <= 10; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i - 1) * 32, (float) (firstY + i * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Down horizontal.
						for (int i = 1; i <= 38; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 9) * 32, (float) (firstY + 11 * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper slope.
						for (int i = 1; i <= 8; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 23) * 32, (float) (firstY - (i + 4) * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper horizontal.
						for (int i = 1; i <= 16; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 31) * 32, (float) (firstY - 12 * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper gate.
						for (int i = 1; i <= 16; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + 47 * 32, (float) (firstY - 12 * 24 - i * 32),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Middle horizontal.
						for (int i = 1; i <= 40; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 7) * 32, (float) (firstY),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Change the land type.
						platformState = 0;
					} else if (platformState == 5) {
						// Three paths - continue lower.
						float firstX = platforms[newestPlatform].getX() + PLATFORM_SIDE;
						float firstY = platforms[newestPlatform].getY();
						// Middle horizontal.
						for (int i = 1; i <= 40; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 7) * 32, (float) (firstY),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Middle gate.
						for (int i = 1; i <= 8; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + 47 * 32, (float) (firstY - i * 32),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper slope.
						for (int i = 1; i <= 8; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 23) * 32, (float) (firstY - (i + 4) * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper horizontal.
						for (int i = 1; i <= 16; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 31) * 32, (float) (firstY - 12 * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Upper gate.
						for (int i = 1; i <= 16; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + 47 * 32, (float) (firstY - 12 * 24 - i * 32),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Down slope.
						for (int i = 1; i <= 10; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i - 1) * 32, (float) (firstY + i * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Down horizontal.
						for (int i = 1; i <= 38; i++) {
							newestPlatform++;
							if (newestPlatform >= platforms.length)
								newestPlatform = 0;
							scene.detachChild(platforms[newestPlatform]);
							physicsWorld.destroyBody(bodies[newestPlatform]);
							platforms[newestPlatform] = new Sprite(firstX + (i + 9) * 32, (float) (firstY + 11 * 24),
									platformTextureRegion, vertexBufferObjectManager);
							bodies[newestPlatform] = PhysicsFactory.createBoxBody(physicsWorld, 
									platforms[newestPlatform], BodyType.StaticBody, wallFixtureDef);
							scene.attachChild(platforms[newestPlatform]);
						}
						// Change the land type.
						platformState = 0;
					}
				}
			}	
		}));
		
		scene.setOnSceneTouchListener(this);
		return this.scene;
	}
	
	private void createControllers() {
	    HUD hud = new HUD();
	    final Sprite leftButton = new Sprite(CAMERA_WIDTH * 0.0625f, CAMERA_HEIGHT * 0.9f,
	    		this.turnTextureRegion, getVertexBufferObjectManager()) {
	        public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	            if (touchEvent.isActionUp()) {
	                direction = !direction;
	            }
	            return true;
	        };
	    };
	    leftButton.setColor(0.7f, 0.7f, 0.7f);
	    hud.registerTouchArea(leftButton);
	    hud.attachChild(leftButton);
	    
	    final Sprite rightButton = new Sprite(CAMERA_WIDTH * 0.8125f, CAMERA_HEIGHT * 0.9f,
	    		this.jumpTextureRegion, getVertexBufferObjectManager()) {
	        public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	            if (touchEvent.isActionUp()) {
	            	// Jump.
	        		if (jumpState == 0) {
	        			playerBody.setLinearVelocity(new Vector2(playerBody.getLinearVelocity().x, jumpVelocity));
	        			jumpState = 1;
	        		}
	            }
	            return true;
	        };
	    };
	    rightButton.setColor(0.7f, 0.7f, 0.7f);
	    hud.registerTouchArea(rightButton);
	    hud.attachChild(rightButton);
	    
	    final Sprite smallButton = new Sprite(CAMERA_WIDTH * 0.3125f, CAMERA_HEIGHT * 0.9f,
	    		this.shrinkTextureRegion, getVertexBufferObjectManager()) {
	        public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	            if (touchEvent.isActionUp()) {
	            	if (playerCounter == 0) {
		            	playerHorizontalVelocity *= 1.5;
		            	playerFace.setScale(0.5f);
		            	physicsWorld.destroyBody(playerBody);
		            	playerBody = PhysicsFactory.createCircleBody(physicsWorld, playerFace, BodyType.DynamicBody, playerFixtureDef);
		            	playerCounter = -10;
	            	}
	            }
	            return true;
	        };
	    };
	    smallButton.setColor(0.7f, 0.7f, 0.7f);
	    hud.registerTouchArea(smallButton);
	    hud.attachChild(smallButton);
	    
	    final Sprite growButton = new Sprite(CAMERA_WIDTH * 0.5625f, CAMERA_HEIGHT * 0.9f,
	    		this.growTextureRegion, getVertexBufferObjectManager()) {
	        public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	            if (touchEvent.isActionUp()) {
	            	// Get big and slower
	            	if (playerCounter == 0) {
		            	playerHorizontalVelocity /= 1.5;
		            	playerFace.setScale(2);
		            	physicsWorld.destroyBody(playerBody);
		            	playerBody = PhysicsFactory.createCircleBody(physicsWorld, playerFace, BodyType.DynamicBody, playerFixtureDef);
		            	playerCounter = +10;
	            	}
	            }
	            return true;
	        };
	    };
	    growButton.setColor(0.7f, 0.7f, 0.7f);
	    hud.registerTouchArea(growButton);
	    hud.attachChild(growButton);
	    
	    // Add passed time.
 		passedTime = new Text(100, 40, font, "Time passed: 0s", new TextOptions(HorizontalAlign.LEFT), 
 				vertexBufferObjectManager);
 		hud.attachChild(passedTime);
	    playerCamera.setHUD(hud);
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, final TouchEvent pSceneTouchEvent) {
	    if (pSceneTouchEvent.isActionDown()) {
	    	// Identify anvil container to update.
			newestAnvil++;
			if (newestAnvil >= anvilFace.length)
				newestAnvil = 0;
			// Detach old object.
			if (anvilFace[newestAnvil] != null) {
				scene.detachChild(anvilFace[newestAnvil]);
				physicsWorld.destroyBody(anvilBody[newestAnvil]);
			}
			// Make anvil in front of player.
			anvilFace[newestAnvil] = new Sprite((playerBody.getPosition().x + 10) * 32, 
					(playerBody.getPosition().y - 10) * 32, this.anvilTextureRegion, vertexBufferObjectManager);
			anvilBody[newestAnvil] = PhysicsFactory.createBoxBody(this.physicsWorld, anvilFace[newestAnvil], 
					BodyType.DynamicBody, playerFixtureDef);
			this.scene.attachChild(anvilFace[newestAnvil]);
			this.physicsWorld.registerPhysicsConnector(new PhysicsConnector(anvilFace[newestAnvil], 
					anvilBody[newestAnvil], true, true));
	    }
	    return false;
	}
	
	void createEnemy(float x, float y) {
		// Identify anvil container to update.
			newestEnemy++;
			if (newestEnemy >= enemyFace.length)
				newestEnemy = 0;
			// Detach old object.
			if (enemyFace[newestEnemy] != null) {
				scene.detachChild(enemyFace[newestEnemy]);
				physicsWorld.destroyBody(enemyBody[newestEnemy]);
			}
			// Make anvil in front of player.
			enemyFace[newestEnemy] = new Sprite(x, y,
					this.enemyTextureRegion, vertexBufferObjectManager);
			enemyBody[newestEnemy] = PhysicsFactory.createBoxBody(this.physicsWorld, enemyFace[newestEnemy], 
					BodyType.DynamicBody, playerFixtureDef);
			this.scene.attachChild(enemyFace[newestEnemy]);
			this.physicsWorld.registerPhysicsConnector(new PhysicsConnector(enemyFace[newestEnemy], 
					enemyBody[newestEnemy], true, true));
			enemyBody[newestEnemy].setLinearVelocity(-10, 0);
	}
}