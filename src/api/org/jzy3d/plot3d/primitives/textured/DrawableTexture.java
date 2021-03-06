package org.jzy3d.plot3d.primitives.textured;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox2d;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.PlaneAxis;
import org.jzy3d.plot3d.primitives.AbstractDrawable;
import org.jzy3d.plot3d.rendering.textures.SharedTexture;
import org.jzy3d.plot3d.rendering.view.Camera;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

/** A {@link DrawableTexture} can only mount its texture while the GL thread is current,
 * so the best is to let draw() automount texture file the first time the resource is
 * required. 
 * When a texture is loaded for the first time, it updates the current view bounds since
 * the texture bounds where up to now unknown and fixed to origin with no width.
 * 
 * A {@link DrawableTexture} requires a color filter (default is white), and a set of coordinates
 * defining the polygon on which the texture should be drawn.
 *  
 * @author Martin
 *
 */
public class DrawableTexture extends AbstractDrawable implements ITranslucent{
	public DrawableTexture(SharedTexture resource){
		this(resource, PlaneAxis.Z, 0, null, null);
	}
	
	public DrawableTexture(SharedTexture resource, PlaneAxis orientation){
		this(resource, orientation, 0, null, null);
	}
	
	public DrawableTexture(SharedTexture resource, PlaneAxis orientation, float axisValue){
		this(resource, orientation, axisValue, null, null);
	}
	
	public DrawableTexture(SharedTexture resource, PlaneAxis orientation, float axisValue, Color filter){
		this(resource, orientation, axisValue, null, filter);
	}
	
	public DrawableTexture(SharedTexture resource, PlaneAxis orientation, float axisValue, List<Coord2d> coords){
		this(resource, orientation, axisValue, coords, null);
	}
	
	public DrawableTexture(SharedTexture resource, PlaneAxis orientation, float axisValue, List<Coord2d> coords, Color filter){
		this.alpha = 1.0f;
		this.resource = resource;
		this.axisValue = axisValue;
		this.orientation = orientation;
		if(filter == null)
			this.filter = Color.WHITE.clone();
		else
			this.filter = filter;
		if( coords != null ){
			mapping = coords;
			initBoundsWithMapping();
		}
		else{
			mapping = getDefaultTextureMapping();
			initBoundsWithMapping();
		}
	}
	
	public Color getColorFilter() {
		return filter;
	}

	public void setColorFilter(Color filter) {
		this.filter = filter;
	}

	@Override
	public void setAlphaFactor(float a) {
		alpha = a;
	}
	
	protected void initBoundsWithMapping(){
		BoundingBox2d box = new BoundingBox2d(mapping);
		float enlarge = 1;
		if(orientation == PlaneAxis.X)
			bbox = new BoundingBox3d( axisValue-enlarge, axisValue+enlarge, box.xmin(), box.xmax(), box.ymin(), box.ymax() );
		else if(orientation == PlaneAxis.Y)
			bbox = new BoundingBox3d( box.xmin(), box.xmax(), axisValue-enlarge, axisValue+enlarge, box.ymin(), box.ymax() );
		else if(orientation == PlaneAxis.Z)
			bbox = new BoundingBox3d(box.xmin(), box.xmax(), box.ymin(), box.ymax(), axisValue-enlarge, axisValue+enlarge );	
		
	}
	
	protected void initBoundsWithResources(){
		float enlarge = 1;
		if(orientation == PlaneAxis.X)
			bbox = new BoundingBox3d( axisValue-enlarge, axisValue+enlarge, -resource.getHalfWidth(), resource.getHalfWidth(), -resource.getHalfHeight(), resource.getHalfHeight() );
		else if(orientation == PlaneAxis.Y)
			bbox = new BoundingBox3d( -resource.getHalfWidth(), resource.getHalfWidth(), axisValue-enlarge, axisValue+enlarge, -resource.getHalfHeight(), resource.getHalfHeight() );
		else if(orientation == PlaneAxis.Z)
			bbox = new BoundingBox3d( -resource.getHalfWidth(), resource.getHalfWidth(), -resource.getHalfHeight(), resource.getHalfHeight(), axisValue-enlarge, axisValue+enlarge );	
		
	}
	
	protected List<Coord2d> getDefaultTextureMapping(){
		List<Coord2d> mapping = new ArrayList<Coord2d>(4);
		mapping.add( new Coord2d(-resource.getHalfWidth(), -resource.getHalfHeight()) );
		mapping.add( new Coord2d(+resource.getHalfWidth(), -resource.getHalfHeight()) );
		mapping.add( new Coord2d(+resource.getHalfWidth(), +resource.getHalfHeight()) );
		mapping.add( new Coord2d(-resource.getHalfWidth(), +resource.getHalfHeight()) );
		return mapping;
	}
	
	/**
	 * Must supply the expected size of texture in 3d coordinates.
	 * @return
	 */
	public static List<Coord2d> getManualTextureMapping(float width, float height, float xoffset, float yoffset){
		List<Coord2d> mapping = new ArrayList<Coord2d>(4);
		mapping.add( new Coord2d(xoffset-width/2, yoffset-height/2) );
		mapping.add( new Coord2d(xoffset+width/2, yoffset-height/2) );
		mapping.add( new Coord2d(xoffset+width/2, yoffset+height/2) );
		mapping.add( new Coord2d(xoffset-width/2, yoffset+height/2) );
		return mapping;
	}
	
	public static List<Coord2d> getManualTextureMapping(float width, float height){
		return getManualTextureMapping(width, height, 0, 0);
	}
	
	public void debugMapping(){
		System.out.println("mapping");
		for(Coord2d c: mapping){
			System.out.println(c);
		}
	}
	
	public BoundingBox3d getBounds(){
		return bbox;
	}
	
	public Coord2d getPlanePosition() {
		return planePosition;
	}

	public void setPlanePosition(Coord2d planePosition) {
		this.planePosition = planePosition;
	}

	public void draw(GL gl, GLU glu, Camera cam){
		if(transform!=null)
			transform.execute(gl);
		
		// Retrieve resource content
		Texture texture = resource.getTexture(gl);
		TextureCoords coords = resource.getCoords();
		
		// Bind texture & set color filter
		texture.bind();
		callWithAlphaFactor(gl, filter, alpha);
		
		// Draw
		before(gl);
		gl.glBegin(GL.GL_QUADS);
		
		if( orientation == PlaneAxis.X ){
			gl.glTexCoord2f(coords.left(), coords.bottom()); 
			gl.glVertex3f( axisValue, mapping.get(0).x, mapping.get(0).y);
			gl.glTexCoord2f(coords.right(), coords.bottom()); 
			gl.glVertex3f( axisValue, mapping.get(1).x, mapping.get(1).y);
			gl.glTexCoord2f(coords.right(), coords.top()); 
			gl.glVertex3f( axisValue, mapping.get(2).x, mapping.get(2).y);
			gl.glTexCoord2f(coords.left(), coords.top()); 
			gl.glVertex3f( axisValue, mapping.get(3).x, mapping.get(3).y);
		}
		else if( orientation == PlaneAxis.Y ){
			gl.glTexCoord2f(coords.left(), coords.bottom()); 
			gl.glVertex3f( mapping.get(0).x, axisValue, mapping.get(0).y);
			gl.glTexCoord2f(coords.right(), coords.bottom()); 
			gl.glVertex3f( mapping.get(1).x, axisValue, mapping.get(1).y);
			gl.glTexCoord2f(coords.right(), coords.top()); 
			gl.glVertex3f( mapping.get(2).x, axisValue, mapping.get(2).y);
			gl.glTexCoord2f(coords.left(), coords.top()); 
			gl.glVertex3f( mapping.get(3).x, axisValue, mapping.get(3).y);
		}
		else if( orientation == PlaneAxis.Z ){
			gl.glTexCoord2f(coords.left(), coords.bottom()); 
			gl.glVertex3f( planePosition.x + mapping.get(0).x, planePosition.y + mapping.get(0).y, axisValue);
			gl.glTexCoord2f(coords.right(), coords.bottom()); 
			gl.glVertex3f( planePosition.x + mapping.get(1).x, planePosition.y + mapping.get(1).y, axisValue);
			gl.glTexCoord2f(coords.right(), coords.top()); 
			gl.glVertex3f( planePosition.x + mapping.get(2).x, planePosition.y + mapping.get(2).y, axisValue);
			gl.glTexCoord2f(coords.left(), coords.top()); 
			gl.glVertex3f( planePosition.x + mapping.get(3).x, planePosition.y + mapping.get(3).y, axisValue);
		}
		
		gl.glEnd();
		after(gl);
	}
	
	protected void before(GL gl){
		gl.glPushMatrix();
		//gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT,GL.GL_NICEST);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		
		//gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
		//gl.glPolygonOffset(1.0f, 1.0f);	
		
		//gl.glEnable(GL.GL_BLEND);
		//gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		 
		//gl.glEnable(GL.GL_ALPHA_TEST);
		//gl.glAlphaFunc(GL.GL_GREATER, 0);
		 
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glTexEnvf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
	}
	
	protected void after(GL gl){
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE,	GL.GL_MODULATE); 
		//gl.glDisable(GL.GL_ALPHA);
		//gl.glDisable(GL.GL_BLEND);
		gl.glPopMatrix();
	}
	
	
	
	protected SharedTexture resource;
	protected PlaneAxis orientation;
	protected float texMatMix[] = {1.0f, 1.0f, 1.0f, 1.0f};
	protected Color filter;
	protected float axisValue;
	protected List<Coord2d> mapping;
	protected float alpha;
	protected Coord2d planePosition = new Coord2d();
}
