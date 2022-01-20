package com.mpaun.game;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import org.andengine.entity.scene.menu.item.SpriteMenuItem;
import org.andengine.entity.scene.menu.MenuScene;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.FsgModel;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.Jsgf;
import edu.cmu.pocketsphinx.JsgfRule;
import edu.cmu.pocketsphinx.NGramModel;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SphinxUtil;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

public class MazeActivity extends SimpleBaseGameActivity implements IAccelerationListener,  IOnSceneTouchListener,
 							RecognitionListener{
	// This code is based on the AndEngine API and will be densely commented
	// in order to aid understanding, as AndEngine is not strongly documented.
	
	// Load voice recognition API.
	static {
        System.loadLibrary("pocketsphinx_jni");
    }
	
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
	private float jumpVelocity = -7f;
	// Movement constrains.
	private int jumpState = 0;
	private boolean direction = true; 
	int playerLastCommand = -1;
	
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
	TiledTextureRegion keyTextureRegion;

	// Create maze.
	// Maze characteristics.
	float hexagonTop = 400f;
	float hexagonMiddle = 800f;
	float hexagonWall = 15f;
	float hexagonHeight = 300f;
	float startX = 0f;
	float startY = 0f;
	int size = 4;
	Hexagon[][] hexs = new Hexagon[size][size];
	ArrayList<Key> keys = new ArrayList<Key>();
	ArrayList<Door> doors = new ArrayList<Door>();
	int keysRemaining;
	int playerCounter = 0;
	
	// Listener.
	SpeechRecognizer recognizer;
	
	// Physics characteristics of walls and player.
	final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0f, 0f, 0f);
	final FixtureDef playerFixtureDef = PhysicsFactory.createFixtureDef(1f, 0f, 0f);

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
		this.bitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 128, 128, TextureOptions.BILINEAR);
		// The graphics for the player are two squares, forming a rectangle stored at (0,0) in the Atlas. 
		this.playerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bitmapTextureAtlas, 
				this, "player.png", 0, 0, 2, 1);
		this.keyTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bitmapTextureAtlas, 
				this, "key2.png", 0, 64, 1, 1);
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
	}

	@Override
	protected Scene onCreateScene() {
		// Keeps count of FPS (debugging purposes).
		// this.mEngine.registerUpdateHandler(new FPSLogger());
		
		// All things in the world (material and immaterial) will be bound to this.
		this.scene = new Scene();
		this.scene.setOnSceneTouchListener(this);
		
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
			}
		}));
		
		// Create associated graph.
		MazeGraph maze = new MazeGraph(4);
		maze.initBinaryTreeMaze();
		maze.breadthFirstLeavesSearch();
		
		for (int i = 1; i <= size; i++)
			for (int j = 1; j <= size; j++) {
				float[] ratios = new float[4];
				if (maze.containsLink(new Node(i, j - 1), new Node(i, j)))
					ratios[0] = 0.5f;
				else
					ratios[0] = 1;
				
				if (maze.containsLink(new Node(i, j), new Node(i + 1, j)))
					ratios[1] = 0.5f;
				else
					ratios[1] = 1;
				
				if (maze.containsLink(new Node(i, j), new Node(i, j + 1)))
					ratios[2] = 0.5f;
				else
					ratios[2] = 1;
				
				if (maze.containsLink(new Node(i - 1, j), new Node(i, j)))
					ratios[3] = 0.5f;
				else
					ratios[3] = 1;
				
				hexs[i - 1][j - 1] = new Hexagon(startX + (i + j - 2) * (hexagonTop + hexagonMiddle) / 2, 
						startY - (i - j) * hexagonHeight / 2,
						hexagonTop, hexagonMiddle, hexagonWall, hexagonHeight, 
						scene, vertexBufferObjectManager, physicsWorld, ratios, wallFixtureDef);
			}
		
		for (int i = 0; i < maze.leaves.size(); i++) {
			keys.add(new Key(hexs[maze.leaves.get(i).x - 1][maze.leaves.get(i).y - 1], keyTextureRegion));
		}
		keysRemaining = maze.leaves.size();
		
		maze.gates();
		for(int  i = 0; i < maze.gates.size(); i++) {
			doors.add(new Door(hexs[maze.gates.get(i).start.x - 1][maze.gates.get(i).start.y - 1],
					hexs[maze.gates.get(i).end.x - 1][maze.gates.get(i).end.y - 1]));
		}
		
		/* The actual collision-checking. */
		scene.registerUpdateHandler(new IUpdateHandler() {
			@Override
			public void reset() { }

			@Override
			public void onUpdate(final float pSecondsElapsed) {
				for (int i = 0; i < keys.size(); i++)
					if (playerFace.collidesWith(keys.get(i).body)) {
						if (i == keys.size() - 1) {
							// Winning Pop up.
							MenuScene menuScene = new MenuScene(playerCamera);
							final SpriteMenuItem highscoresMenuItem = new SpriteMenuItem(1, 
									playerTextureRegion, vertexBufferObjectManager);
							highscoresMenuItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
							menuScene.addMenuItem(highscoresMenuItem);
							menuScene.buildAnimations();
							menuScene.setBackgroundEnabled(true);
							scene.setChildScene(menuScene);
						} else {
							scene.detachChild(keys.get(i).body);
							scene.detachChild(doors.get(i).rect);
							physicsWorld.destroyBody(doors.get(i).rectBody);
							keysRemaining--;
						}
					}		
			}
		});
		
		// Get resource path.
		File appDir;
        try {
            appDir = SphinxUtil.syncAssets(getApplicationContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
		}
		
		// Create configuration
		Config config = Decoder.defaultConfig();
		config.setInt("-maxhmmpf", 10000);
        config.setBoolean("-fwdflat", false);
        config.setBoolean("-bestpath", false);
        config.setFloat("-kws_threshold", 1e-5);
        config.setString("-rawlogdir", appDir.getPath());
        config.setString("-dict", new File(appDir, "models/lm/cmu07a.dic").getPath());
        config.setString("-hmm", new File(appDir, "models/hmm/en-us-semi").getPath());
        
		// Create listener
        recognizer = new SpeechRecognizer(config);
        // Looks for a certain word.
        recognizer.setKws("wakeup_search", "computer");
        
        // Looks for phrases formed by a grammar.
        Jsgf jsgf = new Jsgf(new File(appDir, "models/player.gram").getPath());
        JsgfRule rule = jsgf.getRule("<player.command>");
        int lw = config.getInt("-lw");
        FsgModel fsg = jsgf.buildFsg(rule, recognizer.getLogmath(), lw);
        recognizer.setFsg("player_search", fsg);
        
        // Looks for a certain type of speech.
        NGramModel lm = new NGramModel(new File(appDir, "models/lm/weather.dmp").getPath());
        recognizer.setLm("weather_serch", lm); 
        
        // Start listening for commands.
		recognizer.addListener(this);
		recognizer.setSearch("player_search");
		recognizer.startListening();
		Log.v("game", "asdf listening started");
				
		
		return this.scene;
	}

	@Override
	public void onAccelerationAccuracyChanged(AccelerationData pAccelerationData) {
	}
	
	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
		return true;
	}

	@Override
	public void onAccelerationChanged(AccelerationData pAccelerationData) {
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
	    
	    playerCamera.setHUD(hud);
	}
	
	@Override
	public void onPartialResult(Hypothesis hypothesis) {
		String text = hypothesis.getHypstr();
		
		// Find latest key sequence.
		int jump = text.lastIndexOf("player jump");
		int turn = text.lastIndexOf("player turn");
		int grow = text.lastIndexOf("player grow");
		int small = text.lastIndexOf("player small");

		if (jump > turn && jump > grow && jump > small && jump > playerLastCommand) {
			// Jump.
			playerLastCommand = jump;
			if (jumpState == 0) {
    			playerBody.setLinearVelocity(new Vector2(playerBody.getLinearVelocity().x, jumpVelocity));
    			jumpState = 1;
    		}
			Log.v("moo", " moo here jump");
		} else if (turn > jump && turn > grow && turn > small && turn > playerLastCommand) {
			// Turn
			playerLastCommand = turn;
			direction = !direction;
			Log.v("moo", " moo here turn");
		} else if (grow > small && grow > turn && grow > jump && grow > playerLastCommand) {
			// Grow
			playerLastCommand = grow;
			if (playerCounter == 0) {
            	playerHorizontalVelocity /= 1.5;
            	playerFace.setScale(2);
            	physicsWorld.destroyBody(playerBody);
            	playerBody = PhysicsFactory.createCircleBody(physicsWorld, playerFace, BodyType.DynamicBody, playerFixtureDef);
            	playerCounter = +10;
        	}
			Log.v("moo", " moo here grow");
		} else if (small > grow && small > turn && small > jump && small > playerLastCommand) {
			// Shrink
			playerLastCommand = small;
			if (playerCounter == 0) {
            	playerHorizontalVelocity *= 1.5;
            	playerFace.setScale(0.5f);
            	physicsWorld.destroyBody(playerBody);
            	playerBody = PhysicsFactory.createCircleBody(physicsWorld, playerFace, BodyType.DynamicBody, playerFixtureDef);
            	playerCounter = -10;
        	}
			Log.v("moo", " moo here small");
		}
	}

	@Override
	public void onResult(Hypothesis arg0) {
	}
	
	@Override
	public void onResumeGame() {
		super.onResumeGame();
		this.enableAccelerationSensor(this);
		recognizer.setSearch("player_search");
		recognizer.startListening();
	}

	@Override
	public void onPauseGame() {
		super.onPauseGame();
		this.disableAccelerationSensor();
		recognizer.stopListening();
		recognizer.removeListener(this);
	}
}