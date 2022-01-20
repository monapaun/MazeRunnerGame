package com.mpaun.game;

import java.io.File;
import java.io.IOException;

import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
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
import android.content.Intent;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

public class MenuActivity extends SimpleBaseGameActivity implements IAccelerationListener, RecognitionListener {
	
	// Load voice recognition API.
	static {
        System.loadLibrary("pocketsphinx_jni");
    }
	
	// SCENE VARIABLES
	// Dimensions of the camera window.
	private static final int CAMERA_WIDTH = 1280;
	private static final int CAMERA_HEIGHT = 736;
	
	// Camera window / view into world.
	private Camera staticCamera;
	// Model of the world.
	private Scene scene;
	// Physics interaction control of material (static and mobile) objects in the world.
	private PhysicsWorld physicsWorld;
	// Listener.
	SpeechRecognizer recognizer;
	Context context;
	Body bubbles[];
	boolean gravity = false;
	
	// Handles graphics displaying.
	private VertexBufferObjectManager vertexBufferObjectManager;
	// Bubble graphics.
	private BitmapTextureAtlas bubbleTextureAtlas;
	private TiledTextureRegion bubbleTextureRegion;
	// HUD graphics.
	BitmapTextureAtlas buttonTextureAtlas;
	TiledTextureRegion scoresTextureRegion;
	TiledTextureRegion mazeTextureRegion;
	TiledTextureRegion runTextureRegion;
	TiledTextureRegion settingsTextureRegion;
	TiledTextureRegion helpTextureRegion;
	// Banner graphics.
	BitmapTextureAtlas bannerTextureAtlas;
	TiledTextureRegion bannerTextureRegion;
		
	@Override
	public EngineOptions onCreateEngineOptions() {
		Toast.makeText(this, "Please be patient for loading!", Toast.LENGTH_SHORT).show();
		staticCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_SENSOR, 
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), staticCamera);
	}

	@Override
	protected void onCreateResources() {
		// Load all the graphics used by this Activity.
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("graphics/");
		
		//Bubble resources.
		// 256 * 256 is the area for all the graphics loaded in this Activity in total.
		this.bubbleTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 256, 256, TextureOptions.BILINEAR);
		// The graphics for the bubble are two 32 * 32 squares, forming a 32 * 32 rectangle
		// stored at (0,0) in the Atlas. 
		this.bubbleTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bubbleTextureAtlas, 
				this, "key.png", 0, 0, 1, 1); // 32 * 32
		this.bubbleTextureAtlas.load();
		
		// HUD buttons resources.
		this.buttonTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 368, 736, TextureOptions.BILINEAR);
		this.scoresTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "scores_button.png", 0, 0, 1, 1);
		this.mazeTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "maze_button.png", 0, 184, 1, 1);
		this.runTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "run_button.png", 0, 368, 1, 1);
		this.settingsTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "settings_button.png", 0, 552, 1, 1);
		this.helpTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.buttonTextureAtlas, 
				this, "help_button.png", 184, 552, 1, 1);
		this.buttonTextureAtlas.load();
		
		// Banner resource.
		this.bannerTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 912, 400, TextureOptions.BILINEAR);
		this.bannerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.bannerTextureAtlas, 
				this, "banner2.png", 0, 0, 1, 1);
		this.bannerTextureAtlas.load();
	}

	@Override
	protected Scene onCreateScene() {
		context = this;
		// All things in the world (material and immaterial) will be bound to this.
		this.scene = new Scene();
		// Background graphics.
		scene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
		// Graphics manager.
		vertexBufferObjectManager = this.getVertexBufferObjectManager();
		// Define physics characteristics of whole world.
		this.physicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);
		this.scene.registerUpdateHandler(this.physicsWorld);
		
		// Add buttons.
	    HUD hud = new HUD();
	    Sprite scoresButton = new Sprite(0, 0, this.scoresTextureRegion, vertexBufferObjectManager) {
	    	public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	    		if (touchEvent.isActionUp()) {
	    			Intent intent = new Intent(context, HighScoreChart.class);
	    			startActivity(intent);
	    		};
	    		return true;
	    	};
	    };
	    hud.registerTouchArea(scoresButton);
	    hud.attachChild(scoresButton);
	    
	    Sprite mazeButton = new Sprite(0, 184, this.mazeTextureRegion, vertexBufferObjectManager) {
	    	public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	    		if (touchEvent.isActionUp()) {
	    			Intent intent = new Intent(context, MazeActivity.class);
	    			startActivity(intent);
	    		};
	    		return true;
	    	};
	    };
	    hud.registerTouchArea(mazeButton);
	    hud.attachChild(mazeButton);
	    
	    Sprite runButton = new Sprite(0, 368, this.runTextureRegion, vertexBufferObjectManager) {
	    	public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	    		if (touchEvent.isActionUp()) {
	    			Intent intent = new Intent(context, RunActivity.class);
	    			startActivity(intent);
	    		};
	    		return true;
	    	};
	    };
	    hud.registerTouchArea(runButton);
	    hud.attachChild(runButton);
	    
	    Sprite settingsButton = new Sprite(0, 552, this.settingsTextureRegion, vertexBufferObjectManager) {
	    	public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	    		if (touchEvent.isActionUp()) {
	    			
	    		};
	    		return true;
	    	};
	    };
	    hud.registerTouchArea(settingsButton);
	    hud.attachChild(settingsButton);
	    
	    Sprite helpButton = new Sprite(184, 552, this.helpTextureRegion, vertexBufferObjectManager) {
	    	public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y) {
	    		if (touchEvent.isActionUp()) {
	    			
	    		};
	    		return true;
	    	};
	    };
	    hud.registerTouchArea(helpButton);
	    hud.attachChild(helpButton);
	    staticCamera.setHUD(hud);
		
		// Add walls.
		Rectangle ground = new Rectangle(368, CAMERA_HEIGHT - 2, CAMERA_WIDTH - 368, 2, vertexBufferObjectManager);
		ground.setColor(0.09804f, 0.6274f, 0.8784f);
		Rectangle roof = new Rectangle(368, 0, CAMERA_WIDTH - 368, 2, vertexBufferObjectManager);
		roof.setColor(0.09804f, 0.6274f, 0.8784f);
		Rectangle left = new Rectangle(368, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		left.setColor(0.09804f, 0.6274f, 0.8784f);
		Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		right.setColor(0.09804f, 0.6274f, 0.8784f);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(1f, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.physicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		this.scene.attachChild(ground);
		this.scene.attachChild(roof);
		this.scene.attachChild(left);
		this.scene.attachChild(right);
		
		// Add bubbles to the world.
		FixtureDef playerFixtureDef = PhysicsFactory.createFixtureDef(1f, 0.5f, 0.5f);
		bubbles = new Body[10];
		for (int i = 0; i < 10; i++) {
			Sprite bubbleFace = new Sprite(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2, 
					this.bubbleTextureRegion, vertexBufferObjectManager);
			bubbleFace.setScale(0.4f);
			bubbles[i] = PhysicsFactory.createCircleBody(this.physicsWorld, 
					bubbleFace, BodyType.DynamicBody, playerFixtureDef);
			this.scene.attachChild(bubbleFace);
			this.physicsWorld.registerPhysicsConnector(new PhysicsConnector(bubbleFace, bubbles[i], true, true));
		}
		
		// Add banner.
		Sprite banner = new Sprite(364, 0, this.bannerTextureRegion, vertexBufferObjectManager);
		this.scene.attachChild(banner);
		
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
        Jsgf jsgf = new Jsgf(new File(appDir, "models/dialog.gram").getPath());
        JsgfRule rule = jsgf.getRule("<dialog.command>");
        int lw = config.getInt("-lw");
        FsgModel fsg = jsgf.buildFsg(rule, recognizer.getLogmath(), lw);
        recognizer.setFsg("menu_search", fsg);
        
        // Looks for a certain type of speech.
        NGramModel lm = new NGramModel(new File(appDir, "models/lm/weather.dmp").getPath());
        recognizer.setLm("weather_serch", lm); 
        
        // Start listening for commands.
		recognizer.addListener(this);
		recognizer.setSearch("menu_search");
		recognizer.startListening();
		Log.v("game", "asdf listening started");
		
		// Finish operation.
		return scene;
	}
	
	@Override
	public void onAccelerationAccuracyChanged(final AccelerationData pAccelerationData) {
	}

	@Override
	public void onAccelerationChanged(final AccelerationData pAccelerationData) {
		if (!gravity) {
			final Vector2 gravity = Vector2Pool.obtain(pAccelerationData.getX(), pAccelerationData.getY());
			this.physicsWorld.setGravity(gravity);
			Vector2Pool.recycle(gravity);
		}
	}

	@Override
	public void onResumeGame() {
		super.onResumeGame();
		this.enableAccelerationSensor(this);
		recognizer.setSearch("menu_search");
		recognizer.startListening();
	}

	@Override
	public void onPauseGame() {
		super.onPauseGame();
		this.disableAccelerationSensor();
		recognizer.stopListening();
		recognizer.removeListener(this);
	}

	@Override
	public void onPartialResult(Hypothesis hypothesis) {
		String text = hypothesis.getHypstr();
		// Find latest key sequence.
		int north = text.lastIndexOf("gravity north");
		int south = text.lastIndexOf("gravity south");
		int east = text.lastIndexOf("gravity east");
		int west = text.lastIndexOf("gravity west");
		int empty = text.lastIndexOf("gravity empty");
		int gravX = 0;
		int gravY = 0;
		if (empty > south && empty > north && empty > west && empty > east) 
			gravity = false;
		else
			gravity = true;
		if (gravity) {
			if (north > south && north > east && north > west) {
				gravX = 0;
				gravY = 10;
			} else if (south > north && south > east && south > west) {
				gravX = 0;
				gravY = -10;
			} else if (east > west && east > south && east > north) {
				gravX = 10;
				gravY = 0;
			} else if (west > east && west > south && west > north) {
				gravX = -10;
				gravY = 0;
			}
			for (int i = 0; i < 10; i++) {
				bubbles[i].setLinearVelocity(gravX, gravY);
			}
		}
	}

	@Override
	public void onResult(Hypothesis arg0) {
	}
}