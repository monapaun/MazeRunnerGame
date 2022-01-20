package com.mpaun.game;

public class Edge {
	// A graph edge has start and an end Node.
	
	Node start;
	Node end;
	
	Edge(Node start, Node end) {
		this.start = start;
		this.end = end;
	}
	
	@Override
	public boolean equals(Object object) {
		if ((this.start.equals(((Edge)object).start) && this.end.equals(((Edge)object).end)) || 
				(this.end.equals(((Edge)object).start) && this.start.equals(((Edge)object).end)))
				return true;
		return false;
	}
}
