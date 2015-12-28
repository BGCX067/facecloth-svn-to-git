import distal.*;
import distal.ui.*;
import distal.ui.control.*;
import distal.ui.inline.*;
import distal.ui.screen.*;
import facecloth.*;
import gestalt.*;
import gestalt.p5.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.media.opengl.*;
import net.silentlycrashing.gestures.*;
import oscP5.*;
import netP5.*;
import processing.core.*;
import processing.video.*;
import processing.opengl.*;

/**
 * A faceCloth port for the Distal multi-touch interface.
 */
/* $Id: FaceClothMultiTouch.java 59 2009-01-20 18:44:23Z prisonerjohn $ */
public class FaceClothMultiTouch extends PApplet {
	// TODO REMOVE LIVE CLOTH WHEN GALLERY IS UP
	
	/*--------------------------------------------*/
	/* ATTRIBUTES                                 */
	/*--------------------------------------------*/
	// whether or not the machine can handle advanced features
	public static final boolean THE_FUTURE_IS_HERE = false;
	
	// FBO
	private Ge.FBO fbo;
	private int fboWidth;
	private int fboHeight;

	// touch points
	private TouchPointCollection touchPoints;
	private int recvPort = 9000;
	private int sendPort = 12000;
	private OscP5 oscClient;
	private OscMessage msg;
	private NetAddress recvAddress;

	// user interface
	public static final int BUTTON_WIDTH = 80;
	public static final int BUTTON_HEIGHT = 50;
	public static final int BUTTON_MARGIN = 10;

	private GlobalUnitList gul;
	private UnitSet gestures;
	private Slideshow slideshow;
	private Dock dashboard;

	// cloth settings
	private ImageCloth faceCloth;
	private Capture capture;
	private int clothWidth = 640;
	private int clothHeight = 480;
	private int meshSize = 20;  // !!! this value MUST divide both (width/clothWidth) and (height/clothHeight) !!!
	private int clothColor = color(255, 255, 0);

	private float gravity = 0;
	private float gravityIncrement = 0.1f;
	private float drag = 0.05f;

	private boolean clearFrame = true;
	private boolean fadeFrame = false;
	private boolean drawHandles = false;  // handled externally so that the handles are not drawn in the ImageCloth FBO

	// cloth handles
	int linkedPoints[];
	public static final int SELECTION_RANGE = 50;

	// application mode
	public static final int SNAPSHOT = 0;
	public static final int GALLERY = 1;
	public static final String IMAGES_FOLDER = "savedImages";
	int currMode = SNAPSHOT;
	PImage currGalleryImage;
	int nextImageIndex;

	/*--------------------------------------------*/
	/* SETUP FUNCTIONS                            */
	/*--------------------------------------------*/
	/**
	 * Runs the applet as an application.
	 * <p>This is required to run in "present" mode.</p>
	 *
	 * @param args runtime arguments (not used here)
	 */
	static public void main(String args[]) {
		PApplet.main(new String[] {  
			"--present",
			"--bgcolor=#000000",
			"--present-stop-color=#000000",
			"FaceClothMultiTouch"
		});
	} 

	/**
	 * Sets up the applet.
	 */
	public void setup() {
		// set up the canvas 
		size(1024, 768, OPENGL); 
		smooth();
		frameRate(30);
		noStroke();
		
		if (THE_FUTURE_IS_HERE) {
			hint(ENABLE_OPENGL_4X_SMOOTH);
			PGraphicsOpenGL pgl = (PGraphicsOpenGL)g;
			GL gl = pgl.gl;
			gl.setSwapInterval(1);
		}

		// set up the FBO environment
		Ge.setup(this);
		Ge.camera().position().z = -1*(this.height-(this.height*.135f));
		initFBO(!THE_FUTURE_IS_HERE);
		
		// set up the video capture
		//capture = new Capture(this, clothWidth/4, clothHeight/4);
		capture = new Capture(this, clothWidth/4, clothHeight/4, Capture.list()[1]);
		
		// set up the ImageCloth
		faceCloth = new ImageCloth(this, capture, clothColor, meshSize, clothWidth, clothHeight, gravity, drag);
		
		// set up the touch point collection
		touchPoints = new TouchPointCollection(this, recvPort, !THE_FUTURE_IS_HERE);
		gul = new GlobalUnitList(this, touchPoints);

		// set up the Unit Sets
		gestures = new UnitSet(this, gul);
		buildGestures(); 
		dashboard = new Dock(this, gul, 0, width, 70, color(22, 22, 22), Dock.Position.TOP, Dock.Animation.BOUNCE, !THE_FUTURE_IS_HERE);
		buildDashboard();
		slideshow = new Slideshow(this, gul, 0, width, 160, color(22, 22, 22), Dock.Position.BOTTOM, Dock.Animation.BOUNCE, !THE_FUTURE_IS_HERE);
		buildSlideshow();
		
		// init the handle-TouchPoint links
		linkedPoints = new int[4];
		for (int i=0; i < 4; i++) {
			linkedPoints[i] = -1; 
		}
		
		// set up the mouse2OSC attributes (for testing purposes)
		oscClient = new OscP5(this, sendPort);
		recvAddress = new NetAddress("127.0.0.1", recvPort);
		
		registerPre(this);
	}

	/**
	 * Initializes the frame buffer object.
	 */
	private void initFBO(boolean strict) {
		if (strict) {
			// compute valid value for the FBO width
			float log = (float)(Math.log(width)/Math.log(2));
			if (log == Math.floor(log)) { 	 	 
				fboWidth = width; 	 	 
			} else { 	 	 
				fboWidth = (int)Math.pow(2, Math.floor(log)+1); 	 	 
			} 	 	 
			// compute valid value for the FBO height 	 	 
			log = (float)(Math.log(height)/Math.log(2)); 	 	 
			if (log == Math.floor(log)) { 	 	 
				fboHeight = height; 	 	 
			} else { 	 	 
				fboHeight = (int)Math.pow(2, Math.floor(log)+1); 	 	 
			} 
		} else {
			fboWidth = width;
			fboHeight = height;
		}

		fbo = Ge.fbo(fboWidth, fboHeight);
		fbo.display().material().blendmode = Gestalt.MATERIAL_BLEND_CUSTOM;
    	fbo.display().material().setCustomBlendFunction(GL.GL_SRC_ALPHA, GL.GL_ONE);
    	fbo.display().material().transparent = true;

		fbo.display().position().set(fboWidth/2, fboHeight/2);
	}

	/**
	 * Builds the gesture analysis elements.
	 */
	private void buildGestures() {
		GestureAnalyzer brain = new GestureAnalyzer(this, 30);

		GestureListener dashboardEar = new ConcurrentGestureListener(this, brain, "^(L)$");
		dashboardEar.registerOnAction("toggleDashboard", this);
		GestureListener slideshowEar = new ConcurrentGestureListener(this, brain, "^(R)$");
		slideshowEar.registerOnAction("toggleSlideshow", this);

		GestureArea ga = new VisibleGestureArea(width/2, height-175, width, 30, color(100, 100, 100), color(16, 16, 16), color(160, 160, 160), color(16, 16, 16), brain);
		gestures.addUnit(ga);
	}

	/**
	 * Builds the dashboard.
	 */
	public void buildDashboard() {
		RectButton handleLockButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*0+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT,
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		handleLockButton.setLabel("lock", 5, 15);
		handleLockButton.registerReleaseAction("toggleHandleLock", this);

		RectButton clearFrameButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*1+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT,
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		clearFrameButton.setLabel("clear", 5, 15);
		clearFrameButton.registerReleaseAction("toggleClearFrame", this);

		RectButton fadeFrameButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*2+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT, 
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		fadeFrameButton.setLabel("fade", 5, 15);
		fadeFrameButton.registerReleaseAction("toggleFadeFrame", this);

		RectButton drawScreenButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*3+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT,
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		drawScreenButton.setLabel("screen", 5, 15);
		drawScreenButton.registerReleaseAction("toggleDrawScreen", this);

		RectButton drawHandlesButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*4+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT, 
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		drawHandlesButton.setLabel("handles", 5, 15);
		drawHandlesButton.registerReleaseAction("toggleDrawHandles", this);

		RectButton drawOutlinesButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*5+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT, 
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		drawOutlinesButton.setLabel("outlines", 5, 15);
		drawOutlinesButton.registerReleaseAction("toggleDrawOutlines", this);

		RectButton drawMeshButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*6+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT,
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		drawMeshButton.setLabel("mesh", 5, 15);
		drawMeshButton.registerReleaseAction("toggleDrawMesh", this);

		RectButton galleryButton = new RectButton(
				BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_WIDTH)*7+BUTTON_WIDTH/2, BUTTON_MARGIN+BUTTON_HEIGHT/2, 
				BUTTON_WIDTH, BUTTON_HEIGHT, 
				color(128), color(0), 
				color(128, 128, 0), color(255, 255, 0));
		galleryButton.setLabel("gallery", 5, 15);
		galleryButton.registerReleaseAction("toggleGallery", this);

		/*
        RoundSlider gravitySlider = new RoundSlider(BUTTON_MARGIN+BUTTON_SIZE/2, BUTTON_MARGIN+(BUTTON_MARGIN+BUTTON_SIZE)*6+BUTTON_SIZE/2, 
                BUTTON_SIZE, 
                150, MTRectSlider.VERTICAL,
                color(255, 255, 0), color(128, 128, 0), 
                color(128, 128, 0), color(255, 255, 0));
        gravitySlider.registerPressAction("setGravity", this, new Class[] { Float.TYPE });

        RectSlider gravitySlider = new RectSlider(300, 100, 26, 26, 200, RectSlider.VERTICAL,
                color(255, 255, 0), color(128, 128, 0), 
                color(128, 128, 0), color(255, 255, 0));
        gravitySlider.registerDragAction("setGravity", this, new Class[] { Float.TYPE });
		 */   

		dashboard.addUnit(handleLockButton);
		dashboard.addUnit(clearFrameButton);
		dashboard.addUnit(drawScreenButton);
		dashboard.addUnit(fadeFrameButton);
		dashboard.addUnit(drawHandlesButton);
		dashboard.addUnit(drawOutlinesButton);
		dashboard.addUnit(drawMeshButton);
		dashboard.addUnit(galleryButton);
		//dashboard.addUnit(gravitySlider);
	} 

	/**
	 * Builds the slideshow.
	 */
	public void buildSlideshow() {
		// register the Thumbnail release action
		slideshow.registerThumbReleaseAction("handleThumbnailRelease", this);
		
		// set the file index to use for saving images
		FilenameFilter imgFilter = new FilenameFilter() {
			// only accept TIFF files
			public boolean accept(File dir, String name) {
				if (name.charAt(0) == '.') return false;
				if (name.toLowerCase().endsWith(".tif")) return true;
				return false;
			}
		};
		File imgsFolder = new File(sketchPath, IMAGES_FOLDER);
		// get the index of the last file and increment it to set the next index
		String[] imgFiles = sort(imgsFolder.list(imgFilter));
		nextImageIndex = parseInt(split(imgFiles[imgFiles.length-1], '.')[0])+1;
	}
	
	/*--------------------------------------------*/
	/* RUNTIME FUNCTIONS                          */
	/*--------------------------------------------*/
	/**
	 * Computes the next frame.
	 */
	public void pre() {
		if (currMode == SNAPSHOT) {
			if (capture.available()) {
				// read the next video frame
				capture.read();
			}

			faceCloth.step();
			
			linkPoints();
		}
	}

	/**
	 * Draws the current frame to the screen.
	 */
	public void draw() {
		background(0);

		// draw the interface elements
		slideshow.draw();
		dashboard.draw();
		gestures.draw();
		touchPoints.drawAllTrails();
		
		// messing around with the z pos of some random mesh points
		//faceCloth.offsetMeshPointPosition((int)random(10, 15), (int)random(10, 15), 0, 0, random(20, 100));

		if (currMode == SNAPSHOT) {
			fbo.bind();   
			// draw the active ImageCloth
			if (clearFrame) {
				// erase the previous frame
				background(0);
			} else if (fadeFrame) {
				// fade out the previous frames
				fill(0, 0, 0, 20);
				rect(0, 0, width, height);
			}
			faceCloth.draw();
			fbo.unbind();
			if (drawHandles) faceCloth.drawHandles();
			
		} else {
			fbo.bind();
			// draw the current gallery image
			background(0);
			if (currGalleryImage != null) {  
				image(currGalleryImage, 0, 0);
			}
			fbo.unbind();
		}
	}

	/**
	 * Links the handles with any free TouchPoint that is over it.
	 */
	public void linkPoints() {
		int tpID;
		int tpIndex;
		Point tpCenter;

		// first, clear all old links
		for (int i=0; i < 4; i++) {
			// if the handle is already linked to a TouchPoint
			if ((tpID = linkedPoints[i]) != -1) {
				// if the linked TouchPoint still exists
				if ((tpIndex = touchPoints.indexOfID(tpID)) != -1) {
					// update the handle coordinates
					tpCenter = (touchPoints.get(tpIndex)).getCenter();
					faceCloth.setHandlePosition(i, (float)tpCenter.x, (float)tpCenter.y, 0);
					faceCloth.clearHandleVelocity(i);

				} else {
					// free the handle
					//faceCloth.setHandleVelocity(i, mouseX-pmouseX, mouseY-pmouseY, 0);
					linkedPoints[i] = -1;
				}
			}
		}

		PVector hPos;
		TouchPoint tp;
		boolean tpIsLinked;

		// second, make new links
		for (int i=0; i < 4; i++) {
			// if the handle is not linked to a TouchPoint 
			if ((tpID = linkedPoints[i]) == -1) {
				hPos = faceCloth.getHandlePosition(i);

				// go through the TouchPoints
				for (Iterator<TouchPoint> it = touchPoints.iterator(); it.hasNext();) {
					tp = it.next();
					tpID = tp.getID();

					// check if the current TouchPoint is linked to a handle
					tpIsLinked = false;
					for (int j=0; j < 4; j++) {
						if (linkedPoints[j] == tpID) {
							tpIsLinked = true;

							break;
						}
					}

					// if the selected TouchPoint is not linked
					if (!tpIsLinked) {
						// if the selected TouchPoint and handle are close
						if (tp.isNear(hPos, SELECTION_RANGE)) {
							// link them together
							linkedPoints[i] = tpID; 
						}
					}
				}
			}
		}
	}
	
	/**
	 * Saves the Slideshow Thumbnails to disk.
	 */
    public void saveScreensToDisk() {
    	for (int i=0; i < Slideshow.NUM_THUMBS; i++) {
    		PImage currImg = slideshow.getThumbImage(i);
    		if (currImg != null) {
    			currImg.save(savePath(IMAGES_FOLDER + "/" + nf(nextImageIndex, 4)));
    			nextImageIndex++;
    		}
    	}
    }

	/*--------------------------------------------*/
	/* BUILT-IN EVENT HANDLERS                    */
	/*--------------------------------------------*/
	/**
	 * Listens for keystrokes.
	 */
	public void keyPressed() {
		// physics environment mods
		if (keyCode == UP) {
			gravity += gravityIncrement;
			faceCloth.setGravity(gravity);
		} else if (keyCode == DOWN) {
			gravity -= gravityIncrement;
			faceCloth.setGravity(gravity);
		}

		// reset all
		else if (key == ' ') {
			// set gravity to 0
			gravity = 0;
			faceCloth.setGravity(gravity);

			// place and fix the corner points
			faceCloth.resetHandles();
			faceCloth.fixHandles();
		}  
	}

	/**
	 * Sends a /clear message through OSC to reset all the TouchPoints.
	 */
	public void mouseReleased() {
		msg = new OscMessage("/clear");
		oscClient.send(msg, recvAddress); 
	}

	/**
	 * Creates and sends a new OSC TouchPoint using the mouse coordinates.
	 */
	public void mouseMoved() {
		msg = new OscMessage("/touchpoints");
		msg.add(((float)mouseX)/width);
		msg.add(((float)mouseY)/height);
		msg.add(1.0f);
		msg.add(((float)(abs(pmouseX-mouseX))/width));
		msg.add(((float)(abs(pmouseY-mouseY))/height));

		oscClient.send(msg, recvAddress);
	}

	/*--------------------------------------------*/
	/* GESTURE EVENT HANDLERS                     */
	/*--------------------------------------------*/
	public void toggleSlideshow() { slideshow.toggle(); }
	public void toggleDashboard() { dashboard.toggle(); }

	/*--------------------------------------------*/
	/* BUTTON EVENT HANDLERS                      */
	/*--------------------------------------------*/
	/**
	 * Toggles the handle locking option.
	 * <p>The handles which are linked to a TouchPoint at the time of the call are the only ones which change state</p>
	 */
	public void toggleHandleLock() {
		// go through the links
		for (int i=0; i < 4; i++) {
			// if the handle is already linked to a TouchPoint
			if ((linkedPoints[i]) != -1) {
				if (faceCloth.handleIsFixed(i))
					// free it
					faceCloth.freeHandle(i);
				else
					// fix it
					faceCloth.fixHandle(i);
			}
		}
	}

	// PApplet.draw() options
	public void toggleClearFrame() { 
		clearFrame = !clearFrame; 
		if (clearFrame) fadeFrame = false;
	}
	public void toggleFadeFrame() { 
		fadeFrame = !fadeFrame; 
		if (fadeFrame) clearFrame = false;
	}
	public void toggleDrawHandles() { drawHandles = !drawHandles; }
	
	// ImageCloth.draw() options
	public void toggleDrawOutlines() { faceCloth.setDrawOutlines(!faceCloth.getDrawOutlines()); }
	public void toggleDrawMesh() { faceCloth.setDrawMesh(!faceCloth.getDrawMesh()); }

	/*
    public void setGravity(float r) {
        println("set gravity pressed "+r);
    }
	 */

	/**
	 * Toggles the gallery mode.
	 */
	public void toggleGallery() {
		if (currMode == SNAPSHOT) {
			saveScreensToDisk();
			dashboard.toggle();
		} else {
			slideshow.clearThumbnails(); 
		}
		currMode = (currMode+1)%2;	
	}
	
	/**
	 * Either saves or loads a snapshot of the ImageCloth depending on the current application mode.
	 *
	 * @param i the index of the Thumbnail that was released
	 */
	public void handleThumbnailRelease(int i) {
		if (currMode == SNAPSHOT) {
			// save a snapshot of the ImageCloth FBO at the given position
			fbo.bind();
			PImage cp = get();		
			fbo.unbind();
			slideshow.setThumbImage(i, cp);
			currGalleryImage = null;
			
		} else {
			// load the snapshot at the given position
			currGalleryImage = slideshow.getThumbImage(i);
		}
	}
}
