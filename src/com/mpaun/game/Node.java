package com.mpaun.game;

public class Node {
	// Node coordinates are on an infinite grid of squares.
	// The positive directions are to the right and down.
	
	int x;
	int y;
	
	Node(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object object) {
		if (((Node)object).x == this.x && ((Node)object).y == this.y)
			return true;
		return false;
	}
}