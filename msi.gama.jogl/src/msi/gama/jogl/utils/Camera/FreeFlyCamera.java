package msi.gama.jogl.utils.Camera;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.swing.SwingUtilities;

import msi.gama.jogl.utils.JOGLAWTGLRenderer;
import msi.gama.jogl.utils.Camera.Arcball.Vector3D;

public class FreeFlyCamera extends AbstractCamera {
	
	public Vector3D _forward;
	public Vector3D _left;
	public Vector3D _position;
	public Vector3D _target;
	public double _theta;
	public double _phi;
	
	public double _speed;
	public double _sensivity;
	
	public boolean forward, backward, strafeLeft, strafeRight;
	
	public final static double INIT_Z_FACTOR = 1.5;
	
	public FreeFlyCamera(JOGLAWTGLRenderer renderer)
    {      
    	super(renderer);
    	_forward = new Vector3D();
    	_left = new Vector3D();
    	_position = new Vector3D();
    	_target = new Vector3D();
    	_phi = 0.0;
        _theta = 0.0;
        
        _speed = 0.02;
        _sensivity = 0.4;
        
        forward = false;
        backward = false;
        strafeLeft = false;
        strafeRight = false;
    }
	
	public FreeFlyCamera(double xPos, double yPos, double zPos, double xLPos,
			double yLPos, double zLPos, JOGLAWTGLRenderer renderer) {
		super(xPos, yPos, zPos, xLPos, yLPos, zLPos, renderer);
		// TODO Auto-generated constructor stub
	}
	
  	public void vectorsFromAngles()
    {
    	Vector3D up = new Vector3D(0.0f,0.0f,1.0f); 
    	
	    if (_phi > 89)
	        _phi = 89;
	    else if (_phi < -89)
	        _phi = -89;

	    double r_temp = Math.cos(_phi*Math.PI/180.f);
	    _forward.z = Math.sin(_phi*Math.PI/180.f);
	    _forward.x = r_temp*Math.cos(_theta*Math.PI/180.f);
	    _forward.y = r_temp*Math.sin((_theta*Math.PI)/180.f);
	    
	    _left = Vector3D.crossProduct(up, _forward);
	    _left.normalize();
	    
	//calculate the target of the camera
	    _target = _forward.add(_position.x, _position.y, _position.z);
	    
    }
    
    public void animate()
    {
    	 	if (this.forward) 
    	        _position = _position.add(_forward.scalarMultiply(_speed*200)); //go forward
    	    if (this.backward)
    	    	_position = _position.subtract(_forward.scalarMultiply(_speed*200)); //go backward
    	    if (this.strafeLeft)
    	    	_position = _position.add(_left.scalarMultiply(_speed*200)); //move on the right
    	    if (this.strafeRight) 
    	    	_position = _position.subtract(_left.scalarMultiply(_speed*200)); //move on the left
    	    
    	    _target = _forward.add(_position.x, _position.y, _position.z);
    }
    
    @Override
    public void UpdateCamera(GL gl,GLU glu, int width, int height)
    {
    	
    	float aspect = (float) width / height;
    	    	
		glu.gluPerspective(45.0f, aspect, 0.1f, maxDim*10);
    	glu.gluLookAt(_position.x,_position.y,_position.z,
                _target.x,_target.y,_target.z,
                0.0f,0.0f,1.0f);
    	
    	//animate();
//    	PrintParam();
    }
    
    @Override
  	public void initializeCamera(double envWidth, double envHeight) {

		if (envWidth > envHeight) {
			maxDim = envWidth;
		} else {
			maxDim = envHeight;
		}
		
		if(isModelCentered){
			_position.x = 0;
			_target.x = 0;
			_position.y = -1;
			_target.y = 0;
			_position.z = (float) (maxDim*1.5);
			_target.z = 0;
		}
		else{
			_position.x = envWidth / 2;
			_target.x = envWidth / 2;
			_position.y = envWidth / 2;
			_target.y = envWidth / 2;
			_position.z = (float) (maxDim*1.5);
			_target.z = 0;
		}
		PrintParam();

	}
  	
    @Override
  	public void initialize3DCamera(double envWidth, double envHeight) {
		if (envWidth > envHeight) {
			maxDim = envWidth;
		} else {
			maxDim = envHeight;
		}
		if(isModelCentered){
			_position.x = 0;
			_target.x = 0;
			_position.y = -envHeight  * 1.75+envHeight/2;
			_target.y = -envHeight * 0.5+envHeight/2;
			_position.z = maxDim;
			_target.z = 0;
		}
		else{
			_position.x = envWidth / 2;
			_target.x = envWidth / 2;
			_position.y = -envHeight  * 1.75;
			_target.y = -envHeight * 0.5;
			_position.z = maxDim;
			_target.z = 0;
		}
	}
	
    
	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {		
		if(arg0.getWheelRotation() > 0)
		{
			_position = _position.subtract(_forward.scalarMultiply(_speed*800)); //on recule
			myRenderer.displaySurface.setZoomLevel(myRenderer.camera.getMaxDim() * INIT_Z_FACTOR / getzPos());
			_target = _forward.add(_position.x, _position.y, _position.z); //comme on a boug�, on recalcule la cible fix�e par la cam�ra
		}
		else
		{
			_position = _position.add(_forward.scalarMultiply(_speed*800)); //on avance
			myRenderer.displaySurface.setZoomLevel(myRenderer.camera.getMaxDim() * INIT_Z_FACTOR / getzPos());
			_target = _forward.add(_position.x, _position.y, _position.z); //comme on a boug�, on recalcule la cible fix�e par la cam�ra
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		//check the difference between the current x and the last x position
		int horizMovement = arg0.getX()- lastxPressed;
		// check the difference between the current y and the last y position
		int vertMovement = arg0.getY()  - lastyPressed; 
		
		// set lastx to the current x position
		lastxPressed = arg0.getX() ; 
		// set lastyPressed to the current y position
		lastyPressed = arg0.getY();
		
		_theta -= horizMovement*_sensivity;					
		_phi -= vertMovement*_sensivity;
		
		vectorsFromAngles();
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
//		if (SwingUtilities.isLeftMouseButton(arg0))
//		{
//		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		PrintParam();
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		if ( isArcBallOn(arg0) && isModelCentered ) {
			if ( SwingUtilities.isRightMouseButton(arg0) ) {
				myRenderer.reset();
			}
		} else {
			// myCamera.PrintParam();
			// System.out.println( "x:" + mouseEvent.getX() + " y:" + mouseEvent.getY());
		}
	}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) {
		lastxPressed = arg0.getX();
		lastyPressed = arg0.getY();
		
		//Picking mode
		if(myRenderer.displaySurface.picking)
		{
			//Activate Picking when press and right click and if in Picking mode
			if(SwingUtilities.isRightMouseButton(arg0))
				isPickedPressed = true;	
			
			mousePosition.x = arg0.getX();
			mousePosition.y = arg0.getY();
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if ( myRenderer.displaySurface.selectRectangle ) {
			enableROIDrawing = false;
		}
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		switch (arg0.getKeyCode()) {
		case VK_LEFT: 
			//System.out.println("left arrow");
			strafeLeft = true;
			break;
		case VK_RIGHT: 
			strafeRight = true;
			break;
		case VK_UP:
			forward = true;
			break;
		case VK_DOWN:
			backward = true;
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		switch (arg0.getKeyCode()) {
		case VK_LEFT: // player turns left (scene rotates right)
			strafeLeft = false;
			break;
		case VK_RIGHT: // player turns right (scene rotates left)
			strafeRight = false;
			break;
		case VK_UP:
			forward = false;
			break;
		case VK_DOWN:
			backward = false;
			break;
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		System.out.println("left arrow");
	}
	
	@Override
	public double getzPos() {
		return _position.z;	}
	
	public double getMaxDim() {
		return maxDim;}
	
	@Override
	public void PrintParam() {
		System.out.println("xPos:" + _position.x + " yPos:" + _position.y  + " zPos:" + _position.z );
		System.out.println("xLPos:" + _target.x + " yLPos:" + _target.y + " zLPos:" + _target.z);
		System.out.println("_forwardX:" + _forward.x + " _forwardY:" + _forward.y + " _forwardZ:" + _forward.z);

	}
	

}