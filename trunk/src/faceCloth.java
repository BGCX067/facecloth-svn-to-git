import processing.core.*;
import processing.video.*;
import processing.opengl.*;
import facecloth.*;

/* $Id$ */
public class faceCloth extends PApplet {
    // display variables
    ImageCloth cloth;
    int meshSize = 20;
    int clothWidth = 320;
    int clothHeight = 240;
    int clothColor = color(255, 255, 0);

    Capture capture;
    
    // interaction variables
    boolean clearFrame = true;

    int selectedHandle = -1;
    int selectionRange = 20;

    float gravity = 0;
    float gravityIncrement = 0.1f;
    float drag = 0.05f;

    //--------------------------------------------------------------
    public void setup() {
        // set up the canvas 
        size(1024, 768, OPENGL);
        
        noStroke();
        smooth();
        frameRate(60);
        background(0);

        // set up the video capture
        capture = new Capture(this, clothWidth, clothHeight);

        // set up the ImageCloth
        cloth = new ImageCloth(this, capture, clothColor, meshSize, clothWidth, clothHeight, gravity, drag);
    }

    //--------------------------------------------------------------
    public void draw() {
        if (clearFrame)
            // erase the previous frame
            background(0);

        cloth.step();

        // if required, move one of the corner point
        cloth.setHandlePosition(selectedHandle, mouseX, mouseY, 0);
        cloth.clearHandleVelocity(selectedHandle);

        cloth.draw();
    }

    //--------------------------------------------------------------
    public void captureEvent(Capture c) { 
        // read the next video frame
        c.read(); 
    }

    //--------------------------------------------------------------
    public void mousePressed() {
        PVector v;

        selectedHandle = -1;

        // if the click is close to a corner point, select it
        // we can use a loop here since the handle identifiers range from 0-3
        for (int i=0; i < 4; i++) {
            v = cloth.getHandlePosition(i);
            if ((Math.abs(mouseX-v.x) < selectionRange) && (Math.abs(mouseY-v.y) < selectionRange)) {
                // select the current handle
                selectedHandle = i;
                break;
            } 
        }
    }

    //--------------------------------------------------------------
    public void mouseReleased() {
        // set the selected handle's velocity
        cloth.setHandleVelocity(selectedHandle, mouseX-pmouseX, mouseY-pmouseY, 0);

        // deselect the handle
        selectedHandle = -1;
    }

    //--------------------------------------------------------------
    public void keyPressed() {
        // corner point toggles
        if (key == '1') {
            if (cloth.handleIsFixed(ImageCloth.TOP_LEFT))
                // set the top-left corner free
                cloth.freeHandle(ImageCloth.TOP_LEFT);
            else
                // lock the top-left corner down
                cloth.fixHandle(ImageCloth.TOP_LEFT);

        } else if (key == '2') {
            if (cloth.handleIsFixed(ImageCloth.TOP_RIGHT)) 
                // set the top-right corner free
                cloth.freeHandle(ImageCloth.TOP_RIGHT);
            else
                // lock the top-right corner down
                cloth.fixHandle(ImageCloth.TOP_RIGHT);

        } else if (key == '3') {
            if (cloth.handleIsFixed(ImageCloth.BOTTOM_LEFT))
                // set the bottom-left corner free
                cloth.freeHandle(ImageCloth.BOTTOM_LEFT);
            else
                // lock the bottom-left corner down
                cloth.fixHandle(ImageCloth.BOTTOM_LEFT);

        } else if (key == '4') {
            if (cloth.handleIsFixed(ImageCloth.BOTTOM_RIGHT)) 
                // set the bottom-right corner free
                cloth.freeHandle(ImageCloth.BOTTOM_RIGHT);
            else
                // lock the bottom-right corner down
                cloth.fixHandle(ImageCloth.BOTTOM_RIGHT);
        }

        // physics environment mods
        else if (keyCode == UP) {
            gravity += gravityIncrement;
            cloth.setGravity(gravity);
        } else if (keyCode == DOWN) {
            gravity -= gravityIncrement;
            cloth.setGravity(gravity);
        }

        // display mods
        else if (key == 'z')
            // frame clear toggle
            clearFrame = !clearFrame;
        else if (key == 'x')
            // corner point handle toggle
            cloth.setDrawHandles(!cloth.getDrawHandles());
        else if (key == 'c')
            // mesh outline toggle
            cloth.setDrawOutlines(!cloth.getDrawOutlines());
        else if (key == 'v')
            // mesh toggle
            cloth.setDrawMesh(!cloth.getDrawMesh());

        // reset all
        else if (key == ' ') {
            // set gravity to 0
            gravity = 0;
            cloth.setGravity(gravity);

            // place and fix the corner points
            cloth.resetHandles();
            cloth.fixHandles();
        }
    }
    
    //--------------------------------------------------------------   
    static public void main(String args[]) {
        PApplet.main(new String[] { "faceCloth" });
    }
}
