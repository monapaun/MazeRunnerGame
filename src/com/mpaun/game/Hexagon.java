package com.mpaun.game;

import static org.andengine.extension.physics.box2d.util.constants.PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Mesh;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.color.Color;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
public class Hexagon {
	
	float centerX;
	float centerY;
	float topWidth;
	float middleWidth;
	float height;
	float horizontalWallThickness;
	Scene scene;
	VertexBufferObjectManager vertexBufferObjectManager;
	PhysicsWorld physicsWorld;
	FixtureDef wallFixtureDef;
	
	private Rectangle ceiling;
	private Rectangle floor;
	private Rectangle upperRightWall;
	private Rectangle upperLeftWall;
	private Rectangle lowerRightWall;
	private Rectangle lowerLeftWall;
	float sideWallThickness;
	float sideWallLength;
	float sideWallLengthShortUpper;
	float sideWallLengthShortLower;
	float alpha;
	float beta;
	
	float base1;
	float sideAlpha1;
	float sideBeta1;
	float height1;
	float baseAlpha1;
//	float baseBeta1;
	
	float sideAlpha2;
	float base2;
	float sideBeta2;
	float height2;
	float baseAlpha2;
	float baseBeta2;
		
	Hexagon(float pCenterX, float pCenterY, float pTopWidth, float pMiddleWidth,  
			float pHorizontalWallThickness, float pHeight, 
			Scene pScene, VertexBufferObjectManager pVertexBufferObjectManager,
			PhysicsWorld pPhysicsWorld, float[] openings, FixtureDef pWallFixtureDef) {
		centerX = pCenterX;
		centerY = pCenterY;
		topWidth = pTopWidth;
		middleWidth = pMiddleWidth;
		horizontalWallThickness = pHorizontalWallThickness;
		height = pHeight;
		scene = pScene;
		vertexBufferObjectManager = pVertexBufferObjectManager;
		physicsWorld = pPhysicsWorld;
		// Physics characteristics of every wall.
		wallFixtureDef = pWallFixtureDef;
	
		// Auxiliary angles.
		alpha = (float)Math.atan((middleWidth - topWidth) / height);
		beta = (float) (Math.PI / 2 - alpha);
		
		// Side wall sizes.
		sideWallLength = (height / 2 - horizontalWallThickness) / (float)Math.sin(beta);
		sideWallThickness = horizontalWallThickness * (float)Math.sin(alpha);
		
		// Type 1 triangle is encountered at the intersection of a horizontal wall with a side wall.
		base1 = horizontalWallThickness;
		sideAlpha1 = base1 * (float)Math.cos(alpha);
		sideBeta1 = base1 * (float)Math.sin(alpha);
		height1 = base1 * (float)Math.sin(alpha) * (float)Math.cos(alpha);
		baseAlpha1 = height1 / (float)Math.tan(alpha);
//		baseBeta1 = base1 - baseAlpha1;
		
		// Type 2 triangle is found at the intersection of two side walls.
		sideAlpha2 = sideBeta1;
		base2 = sideAlpha2 / (float)Math.cos(alpha);
		sideBeta2 = sideAlpha2 * (float)Math.tan(alpha);
		height2 = sideAlpha2 * (float)Math.sin(alpha);
		baseAlpha2 = sideAlpha2 * (float)Math.cos(alpha);
		baseBeta2 = base2 - baseAlpha2;
		
		sideWallLengthShortUpper = sideWallLength / 2 + (sideBeta2 - sideAlpha1) / 2;
		sideWallLengthShortLower = sideWallLength / 2 - (sideBeta2 - sideAlpha1) / 2;
		
		// Rectangle(x and y coordinates of upper left corner, width, height, world drawing adjutant).		
		ceiling = new Rectangle(centerX - topWidth / 2, centerY - height / 2, 
				topWidth, horizontalWallThickness, vertexBufferObjectManager);
		PhysicsFactory.createBoxBody(physicsWorld, ceiling, BodyType.StaticBody, wallFixtureDef);
		scene.attachChild(ceiling);
		
		floor = new Rectangle(centerX - topWidth / 2, centerY + height / 2 - horizontalWallThickness, 
				topWidth, horizontalWallThickness, vertexBufferObjectManager);
		PhysicsFactory.createBoxBody(physicsWorld, floor, BodyType.StaticBody, wallFixtureDef);
		scene.attachChild(floor);
		
		float auxLength;
		if (openings[1] < 1) 
			auxLength = sideWallLengthShortUpper;
		else
			auxLength = sideWallLength;
		upperRightWall = new Rectangle(centerX + topWidth / 2 + height1, centerY - height / 2 + baseAlpha1, 
				auxLength, sideWallThickness, vertexBufferObjectManager);
		upperRightWall.setRotationCenter(0, 0);
		upperRightWall.setRotation(180 * beta / (float)Math.PI);
		PhysicsFactory.createBoxBody(physicsWorld, upperRightWall, BodyType.StaticBody, wallFixtureDef);
		scene.attachChild(upperRightWall);
		
		if (openings[2] < 1) 
			auxLength = sideWallLengthShortLower;
		else
			auxLength = sideWallLength;
		lowerRightWall = new Rectangle(centerX + middleWidth / 2 - baseBeta2, centerY + height2,
				auxLength, sideWallThickness, vertexBufferObjectManager);
		lowerRightWall.setRotationCenter(0,  0);
		lowerRightWall.setRotation(90 + 180 * alpha / (float)Math.PI);
		PhysicsFactory.createBoxBody(physicsWorld, lowerRightWall, BodyType.StaticBody, wallFixtureDef);
		scene.attachChild(lowerRightWall);
		
		if (openings[0] < 1) 
			auxLength = sideWallLengthShortUpper;
		else
			auxLength = sideWallLength;
		upperLeftWall = new Rectangle(centerX - topWidth / 2, centerY - height / 2 + horizontalWallThickness,
				auxLength, sideWallThickness, vertexBufferObjectManager);
		upperLeftWall.setRotationCenter(0,  0);
		upperLeftWall.setRotation(90 + 180 * alpha / (float)Math.PI);
		PhysicsFactory.createBoxBody(physicsWorld, upperLeftWall, BodyType.StaticBody, wallFixtureDef);
		scene.attachChild(upperLeftWall);
	
		if (openings[3] < 1) 
			auxLength = sideWallLengthShortLower;
		else
			auxLength = sideWallLength;
		lowerLeftWall = new Rectangle(centerX - middleWidth / 2 + base2, centerY,
				auxLength, sideWallThickness, vertexBufferObjectManager);
		lowerLeftWall.setRotationCenter(0, 0);
		lowerLeftWall.setRotation(180 * beta / (float)Math.PI);
		PhysicsFactory.createBoxBody(physicsWorld, lowerLeftWall, BodyType.StaticBody, wallFixtureDef);
		scene.attachChild(lowerLeftWall);
		
		// TODO: take coordinates in any order(at moment in clockwise direction
		// Type 1 triangles.
		// Top right.
		makeTriangle(centerX + topWidth / 2, centerY - height / 2, 
				centerX + topWidth / 2 + height1, centerY - height / 2 + baseAlpha1,
				centerX + topWidth / 2, centerY - height / 2 + horizontalWallThickness);
		
		// Top left.
		makeTriangle(centerX - topWidth / 2, centerY - height / 2, 
				centerX - topWidth / 2, centerY - height / 2 + horizontalWallThickness,
				centerX - topWidth / 2 - height1, centerY - height / 2 + baseAlpha1);
		
		// Bottom right.
		if (openings[2] == 1)
			makeTriangle(centerX + topWidth / 2, centerY + height / 2,
					centerX + topWidth / 2, centerY + height / 2 - horizontalWallThickness,
					centerX + topWidth / 2 + height1, centerY + height / 2 - baseAlpha1);
		
		// Bottom left.
		if (openings[3] == 1)
			makeTriangle(centerX - topWidth / 2, centerY + height / 2,
					centerX - topWidth / 2 - height1, centerY + height / 2 - baseAlpha1,
					centerX - topWidth / 2, centerY + height / 2 - horizontalWallThickness);
		
		// Type 2 triangles.
		// Top right.
		if (openings[1] == 1)
			makeTriangle(centerX + middleWidth / 2, centerY,
					centerX + middleWidth / 2 - base2, centerY,
					centerX + middleWidth / 2 - baseBeta2, centerY - height2);
		else
			makeTriangle(centerX + middleWidth / 2, centerY,
					centerX + middleWidth / 2 - base2, centerY,
					centerX + middleWidth / 2, centerY - horizontalWallThickness);
		
		// Bottom right.
		makeTriangle(centerX + middleWidth / 2, centerY,
				centerX + middleWidth / 2 - baseBeta2, centerY + height2,
				centerX + middleWidth / 2 - base2, centerY);
		
		// Top left.
		if (openings[0] == 1)
			makeTriangle(centerX - middleWidth / 2, centerY,
					centerX - middleWidth / 2 + baseBeta2, centerY - height2,
					centerX - middleWidth / 2 + base2, centerY);
		else
			makeTriangle(centerX - middleWidth / 2, centerY,
					centerX - middleWidth / 2, centerY - horizontalWallThickness,
					centerX - middleWidth / 2 + base2, centerY);
		
		// Bottom left.
		makeTriangle(centerX - middleWidth / 2, centerY,
				centerX - middleWidth / 2 + base2, centerY,
				centerX - middleWidth / 2 + baseBeta2, centerY + height2);		
	}
	
	Body makeTriangle(float x1, float y1, float x2, float y2, float x3, float y3) {		
		float[] baseBufferData = {x1, y1, 0, x2, y2, 0, x3, y3, 0};
		Mesh mesh = new Mesh(0, 0, baseBufferData, 3, DrawMode.TRIANGLE_FAN, vertexBufferObjectManager);
		mesh.setColor(Color.WHITE);
		final Vector2[] vertices1 = {
				new Vector2(baseBufferData[0] / PIXEL_TO_METER_RATIO_DEFAULT, 
						baseBufferData[1] / PIXEL_TO_METER_RATIO_DEFAULT),
				new Vector2(baseBufferData[3] / PIXEL_TO_METER_RATIO_DEFAULT,
						baseBufferData[4] / PIXEL_TO_METER_RATIO_DEFAULT),
				new Vector2(baseBufferData[6] / PIXEL_TO_METER_RATIO_DEFAULT, 
						baseBufferData[7] / PIXEL_TO_METER_RATIO_DEFAULT),
		};
		Body body = PhysicsFactory.createPolygonBody(physicsWorld, mesh, 
				vertices1, BodyType.StaticBody, wallFixtureDef);
		scene.attachChild(mesh);
		return body;
	}
}