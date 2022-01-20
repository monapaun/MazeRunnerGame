package com.mpaun.game;

import org.andengine.entity.primitive.Rectangle;
import org.andengine.extension.physics.box2d.PhysicsFactory;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

public class Door {

	Hexagon room1, room2;
	Rectangle rect;
	Body meshBody, rectBody;
	
	Door(Hexagon pRoom1, Hexagon pRoom2) {
		room1 = pRoom1;
		room2 = pRoom2;
		
		float width;
		float height;
		float angle;
		float pointX;
		float pointY;
		
		// The width of two side walls.
		width = room2.sideWallLengthShortLower;
		height = room1.sideWallThickness + room2.sideWallThickness;;
		
		// Needs to be perpendicular on the cut side wall.
		if ((room1.centerX - room2.centerX) * (room1.centerY - room2.centerY) < 0) {
			// Direction / based on lower left wall
			angle = room2.beta;
			pointX = room2.centerX - room2.middleWidth / 2 + room2.base2 +
					room2.sideWallLengthShortLower * (float)Math.cos(angle); 
			pointY = room2.centerY + room2.sideWallLengthShortLower * (float)Math.sin(angle);
			
			room2.makeTriangle(pointX + room2.sideWallLengthShortLower * (float)Math.cos(angle),
					pointY + room2.sideWallLengthShortLower * (float)Math.sin(angle),
					room2.centerX - room2.topWidth / 2, 
					room2.centerY + room2.height / 2 - room2.horizontalWallThickness,
					room1.centerX + room1.middleWidth / 2 - room1.base2, room1.centerY);
		} else {
			// Direction \ based on upper left wall.
			angle = (float)(Math.PI / 2) + room2.alpha; 
			pointX = room2.centerX - room2.topWidth / 2 -
					room2.sideWallLengthShortUpper * (float)Math.sin(room2.alpha);
			pointY = room2.centerY - room2.height / 2 + room2.horizontalWallThickness + 
					room2.sideWallLengthShortUpper * (float)Math.cos(room2.alpha);
			
			meshBody = room2.makeTriangle(room2.centerX - room2.middleWidth / 2 + room2.base2,
					room2.centerY,
					room1.centerX + room1.topWidth / 2, 
					room1.centerY + room1.height / 2 - room1.horizontalWallThickness,
					room2.centerX - room2.middleWidth / 2 + room2.base2 - height * (float)Math.cos(room2.alpha),
					room2.centerY - height * (float)Math.sin(room2.alpha));
		}	
		
		rect = new Rectangle(pointX, pointY, width, height, room1.vertexBufferObjectManager);
		rect.setColor(0.3f, 0.3f, 0.3f);
		rect.setRotationCenter(0, 0);
		rect.setRotation(180 * angle / (float)Math.PI);
		rectBody = PhysicsFactory.createBoxBody(room1.physicsWorld, rect, BodyType.StaticBody, room1.wallFixtureDef);
		room1.scene.attachChild(rect);
	}
}
