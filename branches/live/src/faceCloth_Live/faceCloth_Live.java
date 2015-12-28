import processing.video.*;
import processing.core.PApplet;
import processing.opengl.*;
import processing.serial.*;
import facecloth.*;

/* $Id$ */
public class faceCloth_Live extends PApplet {
    // global constants
    static final int X = 0;
    static final int Y = 1;
    static final int POT_MIDPOINT = 4;
    static final int POT_MULTIPLIER = 2;
    static final int COORD_MULTIPLIER = 4;
    static final int EASE = 4;
    static final int RESET_HOLD = 4;

    // serial control constants
    static final int A_LOCK = 0;
    static final int B_LOCK = 1;
    static final int C_LOCK = 2;
    static final int D_LOCK = 3;

    static final int H_TOGGLE = 4;
    static final int E_TOGGLE = 5;
    static final int S_TOGGLE = 6;
    static final int O_TOGGLE = 7;
    static final int M_TOGGLE = 8;

    static final int N_SELECT = 9;
    static final int R_SWITCH = 10;

    static final int X_ACC = 11;
    static final int Y_ACC = 12;
    static final int F_VAL = 13;

    // serial connection variables 
    static final String portName = "/dev/cu.usbserial-3B1";  // for Mac
    //static final String portName = "COM4";  // for Windows
    static final int baudRate = 19200;
    Serial port;
    String buffer= ""; 

    // display variables
    ImageCloth faceCloth;
    int meshSize = 20;  // !!! this value MUST divide both (width/clothWidth) and (height/clothHeight) !!!
    int clothWidth = 400;
    int clothHeight = 300;

    int uiColor = color(255, 255, 0);
    int aHandleColor = color(255, 188, 0);
    int bHandleColor = color(93, 180, 2);
    int cHandleColor = color(247, 10, 10);
    int dHandleColor = color(28, 0, 219);

    Capture capture;
    int sampleW, sampleH;

    // interaction variables
    boolean clearFrame = true;
    int numResets = 0; 
    int currDX = 0;
    int currDY = 0;

    int selectedHandle;

    float gravity = 0;
    float drag = 0.05f;

    //--------------------------------------------------------------
    public void setup() {
        // set up the canvas 
        //size(900, 650, OPENGL);
        size(screen.width, screen.height, OPENGL);
        frame.setTitle("faceCloth_Live"); 

        noStroke();
        smooth(); 
        frameRate(60);

        // set up the video capture
        capture = new Capture(this, clothWidth/4, clothHeight/4);

        sampleW = clothWidth/(meshSize*4);
        sampleH = clothHeight/(meshSize*4);

        // set up the ImageCloth
        faceCloth = new ImageCloth(this, capture, uiColor, meshSize, clothWidth, clothHeight, gravity, drag);
        faceCloth.setHandleColours(aHandleColor, bHandleColor, cHandleColor, dHandleColor);

        // establish the serial port connection
        port = new Serial(this, portName, baudRate);
    }

    //--------------------------------------------------------------
    public void draw() {
        // listen to serial port and trigger serial event  
        while (port.available() > 0) {
            serialEvent(port.read());
        } 

        if (clearFrame)
            // erase the previous frame
            background(100);

        faceCloth.step();

        try {
            moveSelectedHandle();
        } catch (NullPointerException npe) {
            println("ERROR: No Selected Handle Defined");
        }

        faceCloth.draw();
    }

    //--------------------------------------------------------------
    public void captureEvent(Capture c) { 
        // read the next video frame
        c.read(); 
    }

    //--------------------------------------------------------------
    public void serialEvent(int serial) {
        if (serial == '\n') {
            try {
                processSerial(buffer);
            } catch(Exception e) {
                println("ERROR: Malformed Serial Buffer String!");
            }  
            buffer = "";
        } else {
            buffer += (char)serial;
        }
    }

    //--------------------------------------------------------------
    public void processSerial(String data) throws Exception {
        //println(data);

        // get and process the handle lock values
        if (data.charAt(A_LOCK) == '0') faceCloth.fixHandle(ImageCloth.TOP_LEFT);
        else faceCloth.freeHandle(ImageCloth.TOP_LEFT);

        if (data.charAt(B_LOCK) == '0') faceCloth.fixHandle(ImageCloth.TOP_RIGHT);
        else faceCloth.freeHandle(ImageCloth.TOP_RIGHT);

        if (data.charAt(C_LOCK) == '0') faceCloth.fixHandle(ImageCloth.BOTTOM_LEFT);
        else faceCloth.freeHandle(ImageCloth.BOTTOM_LEFT);

        if (data.charAt(D_LOCK) == '0') faceCloth.fixHandle(ImageCloth.BOTTOM_RIGHT);
        else faceCloth.freeHandle(ImageCloth.BOTTOM_RIGHT);

        // get and process the draw option values
        if (data.charAt(E_TOGGLE) == '0') clearFrame = true;
        else clearFrame = false;

        if (data.charAt(H_TOGGLE) == '0') faceCloth.setDrawHandles(true);
        else faceCloth.setDrawHandles(false);

        //if (data.charAt(S_TOGGLE) == '0') faceCloth.setDrawScreen(true);
        //else faceCloth.setDrawScreen(false);

        if (data.charAt(O_TOGGLE) == '0') faceCloth.setDrawOutlines(true);
        else faceCloth.setDrawOutlines(false);

        if (data.charAt(M_TOGGLE) == '0') faceCloth.setDrawMesh(true);
        else faceCloth.setDrawMesh(false);

        // get and process the selected handle value
        if (data.charAt(N_SELECT) == '1') selectedHandle = ImageCloth.TOP_LEFT;
        else if (data.charAt(N_SELECT) == '2') selectedHandle = ImageCloth.TOP_RIGHT;
        else if (data.charAt(N_SELECT) == '3') selectedHandle = ImageCloth.BOTTOM_LEFT;
        else selectedHandle = ImageCloth.BOTTOM_RIGHT; 

        // get and process the selected handle target
        currDX = convertAndScale(data.charAt(X_ACC));
        currDY = convertAndScale(data.charAt(Y_ACC));

        // get and process the gravity value
        if (convertAndScale(data.charAt(F_VAL)) != gravity) {
            gravity = convertAndScale(data.charAt(F_VAL));
            faceCloth.setGravity(gravity);
        }

        // get and process the reset value
        if (data.charAt(R_SWITCH) == '0') numResets++;
        else numResets = 0;    

        // only reset the point positions if reset was held for a certain number of cycles
        if (numResets >= RESET_HOLD) {
            faceCloth.resetHandles();
            numResets = 0;
        }
    }

    //--------------------------------------------------------------
    public int convertAndScale(char val) {
        // convert a char in the range [0, 9] to a scaled signed int
        return (((int)val-(int)'0'-POT_MIDPOINT)*POT_MULTIPLIER);
    }

    //--------------------------------------------------------------
    public void moveSelectedHandle() {
        currDX *= COORD_MULTIPLIER;
        currDY *= COORD_MULTIPLIER;

        if ((currDX == 0) && (currDY == 0)) {
            // reset the selected handle's velocity
            faceCloth.setHandleVelocity(selectedHandle, 0, 0, 0);
        } else {
            // move the selected handle target
            faceCloth.offsetHandlePosition(selectedHandle, currDX, currDY, 0);
            faceCloth.clearHandleVelocity(selectedHandle);

            // reset the position adjustment (until the next serial event)
            currDX = 0;
            currDY = 0;
        }
    }
}
