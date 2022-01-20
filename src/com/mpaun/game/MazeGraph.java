package com.mpaun.game;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import android.util.Log;

public class MazeGraph {
	// Describes a square maze made out of smaller squares or
	// a diamond maze made out of smaller hexagons.
	
	int size;
	ArrayList<Edge> adjacencyList = new ArrayList<Edge>();
	ArrayList<Node> leaves = new ArrayList<Node>();
	ArrayList<Edge> gates = new ArrayList<Edge>();
	
	
	MazeGraph(int size) {
		this.size = size;
	}
	
	void initBinaryTreeMaze() {
		// TODO add directionality
		// directionX = 1 means East
		// directionX = -1 means West
		// directionY = 1 means North
		// directionY = -1 means South
		// the two directions indicate in which directions the gaps can be
		
		// North west Maze.
		// First row will only have west openings.
		for (int j = 2; j <= size; j++)
			adjacencyList.add(new Edge(new Node(1, j - 1), new Node(1, j)));
		
		// First column will only have north openings.
		for (int j = 2; j <= size; j++)
			adjacencyList.add(new Edge(new Node(j - 1, 1), new Node(j, 1)));
		
		for (int i = 2; i <= size; i++)
			for (int j = 2; j <= size; j++) {
				if (Math.random() > 0.5) {
					// Horizontal connection.
					adjacencyList.add(new Edge(new Node(i - 1, j), new Node(i, j)));
				} else {
					// Vertical connection.
					adjacencyList.add(new Edge(new Node(i, j - 1), new Node(i, j)));
				}
			}
	}
	
	boolean containsLink(Node nodeX, Node nodeY){
		return adjacencyList.contains(new Edge(nodeX, nodeY));
	}
	
	// Compute the neighbors of a node.
	ArrayList<Node> neighbours(Node node) {
		ArrayList<Node> list = new ArrayList<Node>();
		if (containsLink(node, new Node(node.x - 1, node.y)))
			list.add(new Node(node.x - 1, node.y));
		if (containsLink(node, new Node(node.x + 1, node.y)))
			list.add(new Node(node.x + 1, node.y));
		if (containsLink(node, new Node(node.x, node.y - 1)))
			list.add(new Node(node.x, node.y - 1));
		if (containsLink(node, new Node(node.x, node.y + 1)))
			list.add(new Node(node.x, node.y + 1));
		return list;
	}
	
	// Identify the leaves DFS - works, unused.
	void depthFirstLeavesSearch() {
		Node startNode = new Node(1,1);
		ArrayList<Node> discovered = new ArrayList<Node>();
		Stack<Node> stack = new Stack<Node>();
		Node auxNode;
		
		stack.push(startNode);
		while (!stack.empty()) {
			auxNode = stack.pop();
			if (!discovered.contains(auxNode)) {
				discovered.add(auxNode);
				int grade = 0;
				if (containsLink(auxNode, new Node(auxNode.x - 1, auxNode.y))) {
					stack.push(new Node(auxNode.x - 1, auxNode.y));
					grade++;
				}
				if (containsLink(auxNode, new Node(auxNode.x + 1, auxNode.y))) {
					stack.push(new Node(auxNode.x + 1, auxNode.y));
					grade++;
				}
				if (containsLink(auxNode, new Node(auxNode.x, auxNode.y - 1))) {
					stack.push(new Node(auxNode.x, auxNode.y - 1));
					grade++;
				}
				if (containsLink(auxNode, new Node(auxNode.x, auxNode.y + 1))) {
					stack.push(new Node(auxNode.x, auxNode.y + 1));
					grade++;
				}
				if (grade == 1)
					leaves.add(auxNode);		
			}
		}
	}
	
	// Identify the leaves BFS - works, used.
	void breadthFirstLeavesSearch() {
		Node startNode = new Node(1,1);
		ArrayList<Node> discovered = new ArrayList<Node>();
		Queue<Node> queue = new LinkedList<Node>();
		Node auxNode;
		queue.add(startNode);
		discovered.add(startNode);
		while (queue.size() > 0) {
			auxNode = queue.remove();
			if (neighbours(auxNode).size() == 1)
				leaves.add(auxNode);
			for (int i = 0; i < neighbours(auxNode).size(); i++) {
				if (!discovered.contains(neighbours(auxNode).get(i))) {
					discovered.add(neighbours(auxNode).get(i));
					queue.add(neighbours(auxNode).get(i));
				}
			}
		}
		for (int i = 0; i < leaves.size(); i++)
			Log.v("game", "moo leave #" + (i + 1) + " is at: (" + leaves.get(i).x + "," + leaves.get(i).y + ")");
	}
	
	@SuppressWarnings("unchecked")
	ArrayList<Node> dijkstraPathSearch(Node stopNode) {
		// Assign impossibly large (infinite) path lengths to all nodes except the starting one. 
		// The cost of each node is in the cell with number: (x - 1) * y - 1.
		int[] cost = new int[size*size];
		for (int i = 0 ; i < size * size; i++)
			cost[i] = size * size;
		// Assign top node of the tree null cost and make it current node.
		Node currentNode = new Node(1, 1);
		cost[0] = 0;
		// Shortest paths to each node.
		ArrayList<Node>[] paths = new ArrayList[size * size];
		for (int i = 0 ; i < size * size; i++)
			paths[i] = new ArrayList<Node>();
		// Mark all other nodes as unvisited.
		ArrayList<Node> unvisited = new ArrayList<Node>();
		for (int i = 1; i <= size; i++)
			for (int j = 1; j <= size; j++)
				unvisited.add(new Node(i, j));
		
		
		while (unvisited.size() > 0) {
			ArrayList<Node> neighbours = neighbours(currentNode);
			unvisited.remove(currentNode);
			// Update potential costs.
			for (int i = 0; i < neighbours.size(); i++) {
				if (cost[(currentNode.x - 1) * size + currentNode.y - 1] + 1 < 
						cost[(neighbours.get(i).x - 1) * size + neighbours.get(i).y - 1]) {
					cost[(neighbours.get(i).x - 1) * size + neighbours.get(i).y - 1] =
						cost[(currentNode.x - 1) * size + currentNode.y - 1] + 1;
					paths[(neighbours.get(i).x - 1) * size + neighbours.get(i).y - 1] = 
							(ArrayList<Node>) paths[(currentNode.x - 1) * size + currentNode.y - 1].clone();
					paths[(neighbours.get(i).x - 1) * size + neighbours.get(i).y - 1].add(currentNode);
				}
			}
			// If the stop node is reached, return its updated cost.
			if (neighbours.contains(stopNode)) {
				paths[(stopNode.x - 1) * size + stopNode.y - 1].add(stopNode);
				return paths[(stopNode.x - 1) * size + stopNode.y - 1];
			}
			
			// Find the unvisited node with smallest cost.
			int min = size * size + 1;
			for (int i = 0; i < unvisited.size(); i++) {
				if (cost[(unvisited.get(i).x - 1) * size + unvisited.get(i).y - 1] < min) {
					min = cost[(unvisited.get(i).x - 1) * size + unvisited.get(i).y - 1];
					currentNode = unvisited.get(i);
				}
			}
		}
		return new ArrayList<Node>() ;
	}
	
	// Detect gates; needs to have leaves already computed.
	void gates() {
		ArrayList<Node> used = dijkstraPathSearch(leaves.get(0));
		for (int i = 1; i < leaves.size(); i++) {
			ArrayList<Node> path = dijkstraPathSearch(leaves.get(i));
			// See first node in it not in used.
			for (int j = 0; j < path.size(); j ++) {
				if(!used.contains(path.get(j))) {
					gates.add(new Edge(path.get(j - 1), path.get(j)));
				Log.v("game", "gate: (" + path.get(j - 1).x + "," +  path.get(j - 1).y + ") to (" +
						path.get(j - 1).x + ";" +  path.get(j - 1).y);
				break;
				}
			}
			// Add all nodes for this leaf in used.
			for (int j = 0; j < path.size(); j ++) {
				if(!used.contains(path.get(j))) {
					used.add(path.get(j));
				}
			}
		}
		
	}
}