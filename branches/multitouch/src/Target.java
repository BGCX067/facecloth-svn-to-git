import processing.core.*;

public class Target {
	
	public static final boolean VISIBLE = true;
	public static final boolean INVISIBLE = true;
	
	private boolean currStatus = INVISIBLE;
	
	private float xPos;
	private float yPos;
	private float zPos;
	
	public Target() {
		
	}
	
	public void setPosition(float nX, float nY, float nZ) {
	    xPos = nX;
	    yPos = nY;
	    zPos = nZ;
	}
	
	public void draw(PApplet p) {
		if(currStatus){
			p.pushMatrix();
			p.translate(xPos, yPos);
			p.fill(255, 255, 0, 70);
			p.smooth();
	    	p.noStroke();
	    	for (int i=0; i < 4; i++) {
	    		p.rotate(p.PI/2*i);
		    	p.bezier(-5, -20,  -3, -23,  3, -23,  5, -20);	
		    	p.triangle(-5, -20, +5, -20, 0, -10);
		    	
	    	}
	    	p.popMatrix();
		}
	}
	
	public void setStatus (boolean val){ currStatus = val;}
	public boolean getStatus (){ return currStatus; }

}
