package facecloth;

import processing.core.*;
import traer.physics.*;

/**
 * A cloth with a PImage mapped onto it.
 */
/* $Id$ */
public class ImageCloth {
	// constants
	public static final int TOP_LEFT = 0;
	public static final int TOP_RIGHT = 1;
	public static final int BOTTOM_LEFT = 2;
	public static final int BOTTOM_RIGHT = 3;

	public static final int MARGIN = 20;

	// environment attributes
	private PApplet p;
	private PImage mappedImage;
	private int foregroundColour;
	private int[] handleColours;

	// display options
	private boolean drawHandles = false;
	private boolean drawOutlines = false;
	private boolean drawMesh = false;

	// physics attributes
	private ParticleSystem physics;
	private Particle[][] particles;
	private int meshSize;
	private float meshWidth;
	private float meshHeight;
	private int sampleW;
	private int sampleH;

	/**
	 * Builds an ImageCloth.
	 * 
	 * @param p the parent PApplet
	 * @param mappedImage the mapped PImage
	 * @param foregroundColour the line colour
	 * @param meshSize the number of rows and columns in the mesg
	 * @param clothWidth the width of the cloth mesh (in pixels)
	 * @param clothHeight the height of the cloth mesh (in pixels)
	 * @param g the physics engine gravity
	 * @param d the physics engine drag
	 */
	public ImageCloth(PApplet p, PImage mappedImage, int foregroundColour, int meshSize, int clothWidth, int clothHeight, float g, float d) {
		this.p = p;
		this.mappedImage = mappedImage;
		this.foregroundColour = foregroundColour;

		this.meshSize = meshSize;

		meshWidth = clothWidth+(clothWidth/meshSize);
		meshHeight = clothHeight/2-(clothHeight/meshSize);

		sampleW = clothWidth/meshSize;
		sampleH = clothHeight/meshSize;

		handleColours = new int[4];
		handleColours[TOP_LEFT] = handleColours[TOP_RIGHT] = handleColours[BOTTOM_LEFT] = handleColours[BOTTOM_RIGHT] = foregroundColour;

		// init the physics engine 
		physics = new ParticleSystem(g, d);
		particles = new Particle[meshSize][meshSize];

		buildMesh();
		resetHandles();
		fixHandles();
	}

	/**
	 * Builds the cloth mesh.
	 */
	public void buildMesh() {
		float meshStepX = (float)(meshWidth/meshSize);
		float meshStepY = (float)(meshHeight/meshSize);

		// create a grid of particles
		for (int i=0; i < meshSize; i++) {
			for (int j=0; j < meshSize; j++) {
				particles[i][j] = physics.makeParticle((float)0.2, i*meshStepX, j*meshStepY, (float)0.0);
				if (i > 0) {
					// add horizontal springs
					physics.makeSpring(particles[i-1][j], particles[i][j], (float)8.0, (float)0.5, meshStepX);
				}
			}
		}

		// add vertical springs    
		for (int i=0; i < meshSize; i++) {
			for (int j=1; j < meshSize; j++) {
				physics.makeSpring(particles[i][j-1], particles[i][j], (float)8.0, (float)0.5, meshStepY);
			}
		}
	}

	/**
	 * Resets and fixes the mesh corner handles to their original position.
	 */
	public void resetHandles() {
		// top-left corner point
		particles[0][0].moveTo(MARGIN, MARGIN, 0);
		// top-right corner point
		particles[meshSize-1][0].moveTo(p.width-MARGIN, MARGIN, 0);
		// bottom-left corner point
		particles[0][meshSize-1].moveTo(MARGIN, p.height-MARGIN, 0);
		// bottom-right corner point
		particles[meshSize-1][meshSize-1].moveTo(p.width-MARGIN, p.height-MARGIN, 0);
	}

	/** 
	 * Fixes a mesh point.
	 * 
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 */
	public void fixMeshPoint(int x, int y) {
		particles[x][y].makeFixed(); 
	}

	/** 
	 * Fixes the selected mesh corner handle.
	 * 
	 * @param handle the handle to fix
	 */
	public void fixHandle(int handle) {
		switch (handle) {
		case TOP_LEFT:
			fixMeshPoint(0, 0);
			break;
		case TOP_RIGHT:
			fixMeshPoint(meshSize-1, 0);
			break;
		case BOTTOM_LEFT:
			fixMeshPoint(0, meshSize-1);
			break;
		case BOTTOM_RIGHT:
			fixMeshPoint(meshSize-1, meshSize-1); 
			break; 
		}
	}

	/** 
	 * Checks if a mesh point is fixed.
	 * 
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 * @return whether or not the mesh point is fixed
	 */
	public boolean meshPointIsFixed(int x, int y) {
		return particles[x][y].isFixed();
	}

	/**
	 * Checks if the selected mesh corner handle is fixed.
	 * 
	 * @param handle the handle to check
	 * @return whether or not the handle is fixed
	 */
	public boolean handleIsFixed(int handle) {
		switch (handle) {
		case TOP_LEFT:
			return meshPointIsFixed(0, 0);
		case TOP_RIGHT:
			return meshPointIsFixed(meshSize-1, 0);
		case BOTTOM_LEFT:
			return meshPointIsFixed(0, meshSize-1);
		case BOTTOM_RIGHT:
			return meshPointIsFixed(meshSize-1, meshSize-1); 
		}

		return false;
	}

	/**
	 * Fixes all the mesh corner handles.
	 */
	public void fixHandles() {
		// top-left corner point
		fixMeshPoint(0, 0);
		// top-right corner point
		fixMeshPoint(meshSize-1, 0);
		// bottom-left corner point
		fixMeshPoint(0, meshSize-1);
		// bottom-right corner point
		fixMeshPoint(meshSize-1, meshSize-1);
	}

	/** 
	 * Frees a mesh point.
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 */
	public void freeMeshPoint(int x, int y) {
		particles[x][y].makeFree(); 
	}

	/** 
	 * Frees the selected mesh corner handle.
	 * 
	 * @param handle the handle to free
	 */
	public void freeHandle(int handle) {
		switch (handle) {
		case TOP_LEFT:
			freeMeshPoint(0, 0);
			break;
		case TOP_RIGHT:
			freeMeshPoint(meshSize-1, 0);
			break;
		case BOTTOM_LEFT:
			freeMeshPoint(0, meshSize-1);
			break;
		case BOTTOM_RIGHT:
			freeMeshPoint(meshSize-1, meshSize-1); 
			break; 
		}
	}

	/**
	 * Sets a mesh point's velocity.
	 * 
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 * @param vx x-value of new velocity
	 * @param vy y-value of new velocity
	 * @param vz z-value of new velocity
	 */
	public void setMeshPointVelocity(int x, int y, float vx, float vy, float vz) {
		particles[x][y].setVelocity(vx, vy, vz); 
	}

	/** 
	 * Sets the selected mesh corner handle's velocity.
	 * 
	 * @param handle the handle to update
	 * @param vx x-value of new velocity
	 * @param vy y-value of new velocity
	 * @param vz z-value of new velocity
	 */
	public void setHandleVelocity(int handle, float vx, float vy, float vz) {
		switch (handle) {
		case TOP_LEFT:
			setMeshPointVelocity(0, 0, vx, vy, vz);
			break;
		case TOP_RIGHT:
			setMeshPointVelocity(meshSize-1, 0, vx, vy, vz);
			break;
		case BOTTOM_LEFT:
			setMeshPointVelocity(0, meshSize-1, vx, vy, vz);
			break;
		case BOTTOM_RIGHT:
			setMeshPointVelocity(meshSize-1, meshSize-1, vx, vy, vz);
			break; 
		}
	}

	/**
	 * Clears a mesh point's velocity.
	 * 
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 */
	public void clearMeshPointVelocity(int x, int y) {
		particles[x][y].velocity().clear(); 
	}

	/** 
	 * Clears the selected mesh corner handle's velocity.
	 * 
	 * @param handle the handle to update
	 */
	public void clearHandleVelocity(int handle) {
		switch (handle) {
		case TOP_LEFT:
			clearMeshPointVelocity(0, 0);
			break;
		case TOP_RIGHT:
			clearMeshPointVelocity(meshSize-1, 0);
			break;
		case BOTTOM_LEFT:
			clearMeshPointVelocity(0, meshSize-1);
			break;
		case BOTTOM_RIGHT:
			clearMeshPointVelocity(meshSize-1, meshSize-1);
			break; 
		}
	}

	/**
	 * Gets the specified mesh point's 2D position.
	 * 
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 * @return the mesh point position as a Point
	 */
	public PVector getMeshPointPosition(int x, int y) {
		return new PVector(particles[x][y].position().x(), particles[x][y].position().y());
	}

	/**
	 * Gets the selected mesh corner handle's 2D position.
	 * 
	 * @param handle the handle to check
	 * @return the handle position as a Point
	 */
	public PVector getHandlePosition(int handle) {
		switch (handle) {
		case TOP_LEFT:
			return getMeshPointPosition(0, 0);
		case TOP_RIGHT:
			return getMeshPointPosition(meshSize-1, 0);
		case BOTTOM_LEFT:
			return getMeshPointPosition(0, meshSize-1);
		case BOTTOM_RIGHT:
			return getMeshPointPosition(meshSize-1, meshSize-1);
		}

		return new PVector(-1, -1);
	}

	/**
	 * Sets the specified mesh point's position.
	 * 
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 * @param nX new x-coordinate of the point
	 * @param nY new y-coordinate of the point
	 * @param nZ new z-coordinate of the point
	 */
	public void setMeshPointPosition(int x, int y, float nX, float nY, float nZ) {
		particles[x][y].moveTo(nX, nY, nZ);
	}

	/**
	 * Sets the selected mesh corner handle's position.
	 * 
	 * @param handle the handle to update
	 * @param nX new x-coordinate of the point
	 * @param nY new y-coordinate of the point
	 * @param nZ new z-coordinate of the point
	 */
	public void setHandlePosition(int handle, float nX, float nY, float nZ) {
		switch (handle) {
		case TOP_LEFT:
			setMeshPointPosition(0, 0, nX, nY, nZ);
			break;
		case TOP_RIGHT:
			setMeshPointPosition(meshSize-1, 0, nX, nY, nZ);
			break;
		case BOTTOM_LEFT:
			setMeshPointPosition(0, meshSize-1, nX, nY, nZ);
			break;
		case BOTTOM_RIGHT:
			setMeshPointPosition(meshSize-1, meshSize-1, nX, nY, nZ);
			break; 
		}
	}

	/**
	 * Offsets the specified mesh point's position.
	 * 
	 * @param x x-index of the point in the mesh
	 * @param y y-index of the point in the mesh
	 * @param dX x-coordinate offset of the point
	 * @param dY y-coordinate offset of the point
	 * @param dZ z-coordinate offset of the point
	 */
	public void offsetMeshPointPosition(int x, int y, float dX, float dY, float dZ) {
		particles[x][y].moveTo(particles[x][y].position().x()+dX, particles[x][y].position().y()+dY, particles[x][y].position().z()+dZ);
	}

	/**
	 * Offsets the selected mesh corner handle's position.
	 * 
	 * @param handle the handle to update
	 * @param dX x-coordinate offset of the point
	 * @param dY y-coordinate offset of the point
	 * @param dZ z-coordinate offset of the point
	 */
	public void offsetHandlePosition(int handle, float dX, float dY, float dZ) {
		switch (handle) {
		case TOP_LEFT:
			offsetMeshPointPosition(0, 0, dX, dY, dZ);
			break;
		case TOP_RIGHT:
			offsetMeshPointPosition(meshSize-1, 0, dX, dY, dZ);
			break;
		case BOTTOM_LEFT:
			offsetMeshPointPosition(0, meshSize-1, dX, dY, dZ);
			break;
		case BOTTOM_RIGHT:
			offsetMeshPointPosition(meshSize-1, meshSize-1, dX, dY, dZ);
			break; 
		}
	}

	/**
	 * Sets the colours of the mesh corner handles.
	 * 
	 * @param tL the top-left handle colour
	 * @param tR the top-right handle colour
	 * @param bL the bottom-left handle colour
	 * @param bR the bottom-right handle colour
	 */
	public void setHandleColours(int tL, int tR, int bL, int bR) {
		handleColours[TOP_LEFT] = tL; 
		handleColours[TOP_RIGHT] = tR;
		handleColours[BOTTOM_LEFT] = bL;
		handleColours[BOTTOM_RIGHT] = bR;   
	}

	/**
	 * Computes the required changes for the next frame.
	 */
	public void step() {
		physics.advanceTime((float)0.1);
	}

	/**
	 * Draws the mapped PImage on the cloth mesh.
	 */
	public void draw() {
		if (drawMesh) p.stroke(foregroundColour);
		else p.noStroke(); 
		p.fill(255);

		// draw quads to make a mesh out of the particles
		p.beginShape(PApplet.QUADS);
		for (int i=0; i < meshSize-1; i++) {
			for (int j=0; j < meshSize-1; j++) {
				// map the video feed to the mesh, one square at a time
				p.texture(mappedImage);

				p.vertex(particles[i][j].position().x(), particles[i][j].position().y(), particles[i][j].position().z(), i*sampleW, j*sampleH);
				p.vertex(particles[i][j+1].position().x(), particles[i][j+1].position().y(), particles[i][j+1].position().z(), i*sampleW, (j+1)*sampleH);
				p.vertex(particles[i+1][j+1].position().x(), particles[i+1][j+1].position().y(), particles[i+1][j+1].position().z(), (i+1)*sampleW, (j+1)*sampleH);
				p.vertex(particles[i+1][j].position().x(), particles[i+1][j].position().y(), particles[i+1][j].position().z(), (i+1)*sampleW, j*sampleH);    
			}
		}
		p.endShape();

		if (drawOutlines) drawOutlines();     
		if (drawHandles) drawHandles();
	}

	/**
	 * Draws an outline around the mesh.
	 */
	public void drawOutlines() {
		p.stroke(foregroundColour);
		// horizontal lines
		for (int i=0; i < meshSize-1; i++) {
			p.line(particles[i][0].position().x(), particles[i][0].position().y(), particles[i+1][0].position().x(), particles[i+1][0].position().y());
			p.line(particles[i][meshSize-1].position().x(), particles[i][meshSize-1].position().y(), particles[i+1][meshSize-1].position().x(), particles[i+1][meshSize-1].position().y());
		}

		// vertical lines
		for (int j=0; j < meshSize-1; j++) {
			p.line(particles[0][j].position().x(), particles[0][j].position().y(), particles[0][j+1].position().x(), particles[0][j+1].position().y());
			p.line(particles[meshSize-1][j].position().x(), particles[meshSize-1][j].position().y(), particles[meshSize-1][j+1].position().x(), particles[meshSize-1][j+1].position().y());
		}
		p.noStroke(); 
	}

	/**
	 * Draws handles at each corner point of the mesh.
	 */
	public void drawHandles() {
		p.fill(handleColours[TOP_LEFT]);
		drawHandle(particles[0][0].position().x(), particles[0][0].position().y());
		p.fill(handleColours[TOP_RIGHT]);
		drawHandle(particles[meshSize - 1][0].position().x(), particles[meshSize - 1][0].position().y());
		p.fill(handleColours[BOTTOM_LEFT]);
		drawHandle(particles[0][meshSize - 1].position().x(), particles[0][meshSize - 1].position().y());
		p.fill(handleColours[BOTTOM_RIGHT]);
		drawHandle(particles[meshSize - 1][meshSize - 1].position().x(), particles[meshSize - 1][meshSize - 1].position().y());
		p.fill(255); 
	}
	
	/**
	 * Draws a handle at the given point.
	 * 
	 * @param x the x-coord of the handle
	 * @param y the y-coord of the handle
	 */
	public void drawHandle(float x, float y) {
		p.pushMatrix();
		p.translate(x, y);
		p.noStroke();
		for (int i=0; i < 4; i++) {
			p.rotate(PApplet.PI/2*i);
			p.bezier(-4, -17,  -3, -20,  3, -20,  4, -17);	
			p.triangle(-4, -17, 4, -17, 0, -5);
		}
		p.popMatrix();
	}

	public void setGravity(float g) { physics.setGravity(g); }

	public void setDrawHandles(boolean val) { drawHandles = val; }
	public boolean getDrawHandles() { return drawHandles; }
	public void setDrawOutlines(boolean val) { drawOutlines = val; }
	public boolean getDrawOutlines() { return drawOutlines; }
	public void setDrawMesh(boolean val) { drawMesh = val; }
	public boolean getDrawMesh() { return drawMesh; }
}
