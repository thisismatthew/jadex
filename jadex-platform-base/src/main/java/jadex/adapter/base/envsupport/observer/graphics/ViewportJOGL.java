package jadex.adapter.base.envsupport.observer.graphics;

import jadex.adapter.base.envsupport.math.IVector2;
import jadex.adapter.base.envsupport.math.Vector2Double;
import jadex.adapter.base.envsupport.observer.graphics.drawable.DrawableCombiner;
import jadex.adapter.base.envsupport.observer.graphics.layer.ILayer;
import jadex.bridge.ILibraryService;
import jadex.commons.SUtil;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;


/**
 * OpenGL/JOGL-based Viewport. This viewport attempts to use OpenGL for drawing.
 * Exceptions/Errors may be thrown if OpenGL cannot be linked, also be sure to
 * test isValid() afterwards to verify the availability of necessary extensions.
 */
public class ViewportJOGL extends AbstractViewport
{
	/** Clamped texture cache. */
	private Map					clampedTextureCache_;

	/** Repeating texture cache. */
	private Map					repeatingTextureCache_;

	/** Display lists. */
	private Map					displayLists_;

	/** True, until the OpenGL context is initialized. */
	private volatile boolean	uninitialized_;

	/** This will be true if the OpenGL context supports all necessary extensions. */
	private volatile boolean	valid_;

	/** True, if non-power-of-two texture support is available. */
	private boolean				npot_;

	/** Action that renders the frame. */
	private Runnable			renderFrameAction_;
	
	/** Current OpenGL rendering context */
	private GL context_;

	/**
	 * Creates a new OpenGL-based viewport. May throw UnsatisfiedLinkError and
	 * RuntimeException if linking to OpenGL fails.
	 * 
	 * @param title Title of the window.
	 * @param fps target frames per second or no autmatic refresh if zero
	 * @param libService library service for loading resources.
	 */
	public ViewportJOGL(ILibraryService libService)
	{
		super();

		libService_ = libService;
		uninitialized_ = true;
		valid_ = false;
		npot_ = false;
		clampedTextureCache_ = Collections.synchronizedMap(new HashMap());
		repeatingTextureCache_ = Collections.synchronizedMap(new HashMap());
		displayLists_ = Collections.synchronizedMap(new HashMap());

		try
		{
			JOGLNativeLoader.loadJOGLLibraries();
			GLCapabilities caps = new GLCapabilities();
			caps.setDoubleBuffered(true);
			caps.setHardwareAccelerated(true);
			canvas_ = new ResizeableGLCanvas(caps);
			((GLCanvas)canvas_).addGLEventListener(new GLController());
		}
		catch(GLException e)
		{
			throw e;
		}
		catch(Error e)
		{
			throw e;
		}

		canvas_.addMouseListener(new MouseController());

		setSize(new Vector2Double(1.0));
		renderFrameAction_ = new Runnable()
		{
			public void run()
			{
				((GLCanvas)ViewportJOGL.this.canvas_).display();
			}
		};
	}

	public void refresh()
	{
		EventQueue.invokeLater(renderFrameAction_);
	}

	/**
	 * Verifies the OpenGL context is valid and useable.
	 */
	public boolean isValid()
	{
		//TODO: Hack: do proper validity checking
		return true;
		/*while(uninitialized_)
		{
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException e)
			{
			}
		}
		return valid_;*/
	}

	/**
	 * Returns a repeating texture.
	 * 
	 * @param gl OpenGL interface
	 * @param path resource path of the texture
	 * @return the texture
	 */
	public Texture2D getRepeatingTexture(GL gl, String path)
	{

		Texture2D texture = (Texture2D)repeatingTextureCache_.get(path);
		if(texture == null)
		{
			texture = loadTexture(gl, path, GL.GL_REPEAT);
			repeatingTextureCache_.put(path, texture);
		}

		return texture;
	}

	/**
	 * Returns a clamped texture.
	 * 
	 * @param gl OpenGL interface
	 * @param path resource path of the texture
	 * @return the texture
	 */
	public Texture2D getClampedTexture(GL gl, String path)
	{

		Texture2D texture = (Texture2D)clampedTextureCache_.get(path);
		if(texture == null)
		{
			texture = loadTexture(gl, path, GL.GL_CLAMP_TO_EDGE);
			clampedTextureCache_.put(path, texture);
		}

		return texture;
	}

	/**
	 * Returns a previous generated display list or null if it doesn't exist
	 * 
	 * @param listName name of the list
	 * @return previously generated display list
	 */
	public Integer getDisplayList(String listName)
	{
		return (Integer)displayLists_.get(listName);
	}

	/**
	 * Sets a display list.
	 * 
	 * @param listName name of the list
	 * @param list the display list
	 */
	public void setDisplayList(String listName, Integer list)
	{
		displayLists_.put(listName, list);
	}

	/**
	 * Configures the texture matrix for a specific texture. Caller must ensure
	 * being in the correct matrix mode before calling this method.
	 * 
	 * @param gl GL context
	 * @param maxX maximum texture x-coordinate (for padded textures)
	 * @param maxY maximum texture y-coordinate (for padded textures)
	 */
	public void setupTexMatrix(GL gl, float maxX, float maxY)
	{
		gl.glLoadIdentity();
		gl.glTranslatef(maxX * inversionFlag_.getXAsInteger(), maxY
				* inversionFlag_.getYAsInteger(), 0.0f);
		gl.glScalef(maxX * -((inversionFlag_.getXAsInteger() << 1) - 1), maxY
				* -((inversionFlag_.getYAsInteger() << 1) - 1), 1.0f);
	}
	
	/**
	 * Returns the current GL rendering context.
	 * @return GL context, null if none is available
	 */
	public GL getContext()
	{
		return context_;
	}

	private void setupMatrix(GL gl)
	{
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glOrtho(paddedSize_.getXAsDouble() * inversionFlag_.getXAsInteger(),
				paddedSize_.getXAsDouble()
						* (inversionFlag_.getXAsInteger() ^ 1), paddedSize_
						.getYAsDouble()
						* inversionFlag_.getYAsInteger(), paddedSize_
						.getYAsDouble()
						* (inversionFlag_.getYAsInteger() ^ 1), -0.5, 0.5);
		gl.glTranslated(-posX_, -posY_, 0.0);

		// Setup the scissor box
		double xFac = canvas_.getWidth() / paddedSize_.getXAsDouble();
		double yFac = canvas_.getHeight() / paddedSize_.getYAsDouble();
		int x = (int)(-posX_ * xFac);
		int y = (int)(-posY_ * yFac);
		//TODO: this is likely to be wrong
		int w = (int)Math.round(size_.getXAsDouble() * xFac) + 2;
		int h = (int)Math.round(size_.getYAsDouble() * yFac) + 2;
		gl.glScissor(x, y, w, h);
	}

	/**
	 * Loads a Texture
	 * 
	 * @param gl OpenGL interface
	 * @param path texture resource path
	 * @param wrapParam wrap parameter
	 */
	private synchronized Texture2D loadTexture(GL gl, String path, int wrapMode)
	{
		// Load image
		ClassLoader cl = libService_.getClassLoader();

		BufferedImage tmpImage = null;
		try
		{
			tmpImage = ImageIO.read(SUtil.getResource(path, cl));
//			tmpImage = ImageIO.read(cl.getResource(path));
			AffineTransform tf = AffineTransform.getScaleInstance(1, -1);
			tf.translate(0, -tmpImage.getHeight());
			AffineTransformOp op = new AffineTransformOp(tf,
					AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			tmpImage = op.filter(tmpImage, null);
		}
		catch(Exception e)
		{
			System.err.println("Image not found: " + path);
			throw new RuntimeException("Image not found: " + path);
		}

		int width = tmpImage.getWidth();
		int height = tmpImage.getHeight();
		if(!npot_)
		{
			width = (int)Math.pow(2, Math.ceil(Math.log(width) / Math.log(2)));
			height = (int)Math
					.pow(2, Math.ceil(Math.log(height) / Math.log(2)));
		}
		double maxX = (double)(tmpImage.getWidth() - 1) / width;
		double maxY = (double)(tmpImage.getHeight() - 1) / height;

		// Convert image data
		ColorModel colorModel = new ComponentColorModel(ColorSpace
				.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, true,
				false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);

		WritableRaster raster = Raster.createInterleavedRaster(
				DataBuffer.TYPE_BYTE, width, height, 4, null);
		BufferedImage image = new BufferedImage(colorModel, raster, false,
				new Hashtable());
		Graphics2D g = (Graphics2D)image.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);
		g.setComposite(AlphaComposite.Src);
		int ow = tmpImage.getWidth();
		int oh = tmpImage.getHeight();
		g.drawImage(tmpImage, 0, 0, null);

		// Fill up padded textures, may be wrong approach for
		// clamped textures.
		// g.drawImage(tmpImage, ow, 0, null);
		// g.drawImage(tmpImage, 0, oh, null);
		// g.drawImage(tmpImage, ow, oh, null);

		g.dispose();
		tmpImage = null;

		byte[] imgData = ((DataBufferByte)image.getRaster().getDataBuffer())
				.getData();
		ByteBuffer buffer = ByteBuffer.allocateDirect(imgData.length);
		buffer.order(ByteOrder.nativeOrder());
		buffer.put(imgData, 0, imgData.length);
		buffer.flip();

		// Prepare texture
		int[] texId = new int[1];
		gl.glGenTextures(1, texId, 0);
		Texture2D texture = new Texture2D(texId[0], (float)maxX, (float)maxY);

		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glBindTexture(GL.GL_TEXTURE_2D, texId[0]);

		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapMode);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapMode);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_LINEAR);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_COMPRESSED_RGBA, image
				.getWidth(), image.getHeight(), 0, GL.GL_RGBA,
				GL.GL_UNSIGNED_BYTE, buffer);

		gl.glDisable(GL.GL_TEXTURE_2D);


		return texture;
	}

	private class GLController implements GLEventListener
	{
		public void display(GLAutoDrawable drawable)
		{
			GL gl = drawable.getGL();

			setupMatrix(gl);

			gl.glClear(gl.GL_COLOR_BUFFER_BIT);

			gl.glEnable(GL.GL_SCISSOR_TEST);
			
			context_ = gl;

			synchronized(preLayers_)
			{
				for (int i = 0; i < preLayers_.length; ++i)
				{
					ILayer l = preLayers_[i];
					if (!drawObjects_.contains(l))
					{
						l.init(ViewportJOGL.this, gl);
					}
					l.draw(size_, ViewportJOGL.this, gl);
				}
			}
			
			synchronized(objectList_)
			{
				synchronized(objectLayers_)
				{
					objectLayers_.clear();
					for (Iterator it = objectList_.iterator(); it.hasNext(); )
					{
						Object[] o = (Object[]) it.next();
						DrawableCombiner d = (DrawableCombiner) o[1];
						if (!drawObjects_.contains(d))
						{
							d.init(ViewportJOGL.this);
							drawObjects_.add(d);
						}
						objectLayers_.addAll(d.getLayers());
					}
					
					
					gl.glPushMatrix();
					gl.glTranslatef(objShiftX_, objShiftY_, 0.0f);
					for(Iterator it = objectLayers_.iterator(); it.hasNext();)
					{
						Integer layer = (Integer)it.next();
						Iterator it2 = objectList_.iterator();
						while(it2.hasNext())
						{
							Object[] o = (Object[])it2.next();
							Object obj = o[0];
							DrawableCombiner d = (DrawableCombiner)o[1];
							d.draw(obj, layer, ViewportJOGL.this);
						}
					}
					gl.glPopMatrix();
				}
			}

			synchronized(postLayers_)
			{
				for (int i = 0; i < postLayers_.length; ++i)
				{
					ILayer l = postLayers_[i];
					if (!drawObjects_.contains(l))
					{
						l.init(ViewportJOGL.this, gl);
					}
					l.draw(size_, ViewportJOGL.this, gl);
				}
			}
			
			context_ = null;
			gl.glDisable(GL.GL_SCISSOR_TEST);
		}

		public void displayChanged(GLAutoDrawable drawable,
				boolean modeChanged, boolean deviceChanged)
		{
		}

		public void init(GLAutoDrawable drawable)
		{
			GL gl = drawable.getGL();
			context_ = gl;
			gl.glViewport(0, 0, canvas_.getWidth(), canvas_.getHeight());
			gl.glMatrixMode(GL.GL_MODELVIEW);

			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

			setupMatrix(gl);
			gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			
			// Check for OpenGL version or extensions if needed
			if(gl.isExtensionAvailable("GL_VERSION_2_0")
					|| gl.isExtensionAvailable("GL_VERSION_2_1")
					|| gl
							.isExtensionAvailable("GL_ARB_texture_non_power_of_two"))
			{
				npot_ = true;
			}

			// TODO: Add checks.
			valid_ = true;

			/**
			 * if (!(gl.isFunctionAvailable("glGenBuffers"))) { valid_ = false;
			 * } else { }
			 */

			clampedTextureCache_.clear();
			repeatingTextureCache_.clear();
			displayLists_.clear();

			drawObjects_.clear();

			uninitialized_ = false;
			context_ = null;
		}

		public void reshape(GLAutoDrawable drawable, int x, int y, int width,
				int height)
		{
			GL gl = drawable.getGL();
			setSize(size_);
			setupMatrix(gl);
		}

	}

	private class ResizeableGLCanvas extends GLCanvas
	{
		public ResizeableGLCanvas(GLCapabilities caps)
		{
			super(caps);
		}

		public Dimension minimumSize()
		{
			return new Dimension(1, 1);
		}

		public Dimension getMinimumSize()
		{
			return new Dimension(1, 1);
		}
	}
}
