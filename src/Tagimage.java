import javax.microedition.lcdui.*; 

/* 
* Platform specfic (JME MIDP2.0) interface to image pixel reads 
*/

public class Tagimage {

	private int rgb[];

	private int width, height;
	private boolean debug = false;
	private Image image = null;
	private int blockindex = -1;
	private int SIZE = 48; //config.THRESHOLD_WINDOW_SIZE
	private Config config = null;
	private boolean valid = false;
	private boolean initialised  = false;

	//FIXME Should match Config.PIXMAP_SCALE_SIZE
	private static final int PIXMAP_SCALE_SIZE=320;

	public Tagimage(Config _config) { 
		config = _config;
	}

	public Tagimage(Image _image) { 
		init(_image, false);
	}

	public Tagimage(Image _image, boolean caching) { 
		init(_image, caching);
	}

	public void init(Image _image, boolean caching) { 
			image = _image;
			width       = image.getWidth();
			height      = image.getHeight();
			if(debug) System.out.println("image " + width + "x" + height );
			if( width > 0 && height > 0 ) {
				valid = true;
				resizeImage(PIXMAP_SCALE_SIZE); 
				initPixels();
			} 
		    if(caching == false) image = null;
	}

	public Tagimage(String file) { 
		try{
			image = Image.createImage(file);
			width       = image.getWidth();
			height      = image.getHeight();
			if(debug) System.out.println("image " + width + "x" + height );
			valid = true;
			initPixels();
		}catch(java.io.IOException e){
			System.out.println( "Image Load Fail :" +  e);
			valid = false;
		}
	}

	public void initPixels(){
		if(initialised) return;

		width       = image.getWidth();
		height      = image.getHeight();
		rgb = new int[width*height];
		image.getRGB(rgb, 0, width, 0, 0, width, height);
		if(debug) System.out.println("pixels " + width + "x" + height );
		initialised = true;
	}

	public void removeCache(){
		image = null;
	}

	public void dispose() {
		image = null;
		valid = false;
		rgb = null;
	}

	int getWidth(){
		return width;
	}

	int getHeight(){
		return height;
	}

	int _getPixel(int x, int y){
		System.out.print(  x + "," + y );
		if( x < 0 || x > width || y < 0 || y > height) return 0;
		int pix[] = new int[1];
		image.getRGB(pix,0,width,x,y,1,1);
		int pixel  = pix[0];
		System.out.println( " = " + pixel );
		return ( ((pixel & 0x00ff0000) >> 16) + 
			((pixel & 0x0000ff00) >> 8) +
			(pixel & 0x000000ff) );
	}

	int getPixel(int x, int y){
		int pixel  = rgb[(y*width)+x];
		return ( ((pixel & 0x00ff0000) >> 16) + 
			((pixel & 0x0000ff00) >> 8) +
			(pixel & 0x000000ff) );
	}

	int getPixelR(int x, int y){
		int pixel  = rgb[(y*width)+x];
		return ((pixel & 0x00ff0000) >> 16) ;
	}

	int getPixelG(int x, int y){
		int pixel  = rgb[(y*width)+x];
		return ((pixel & 0x0000ff00) >> 8) ;
	}

	int getPixelB(int x, int y){
		int pixel  = rgb[(y*width)+x];
		return (pixel & 0x000000ff) ;
	}

	boolean isValid()
	{
		return valid;	
	}

	int maxRGB()
	{
		return 255;
	}

	Image getImage()
	{
		return image;
	}

	void resizeImage(int size){
		if(image != null ) image = getResizedImage(size);
	}

	Image getResizedImage(int size){
		if(image == null) return null;
		int scale  = width > height ?
                (width*100)/size :
				(height*100)/size;
		int new_width  = (width*100)/scale;
		int new_height = (height*100)/scale;

	    if(debug) System.out.println( width + "->" + new_width + " " + 
						height + "->" + new_height + " (" + scale + ")");

		if( new_width >= width || new_height >= height ) return image;

		int ratio = (width << 16) / new_width;
		Image resizedImage = Image.createImage(new_width, new_height);
		Graphics g = resizedImage.getGraphics();

		for (int x = 0; x < new_width; x++) {
			for (int y = 0; y < new_height; y++) {
				g.setClip(x, y, 1, 1);
				g.drawImage(image,  x - ((x*scale)/100), y - ((y*scale)/100), Graphics.LEFT | Graphics.TOP);
			}
		}
		return resizedImage;
	}
	
	void drawImage(Graphics g, int cw, int ch, int ideal_ch){
		if(g == null) return;
		if(initialised == false) return;
		if(rgb == null) return;
		int w = cw;
		int h = ideal_ch;
		int screenspan = (w<h) ? w : h;
		screenspan-=6; //3 pixel border 
		int scale = 4;										//1/4
		if(PIXMAP_SCALE_SIZE/scale > screenspan)  scale*=2;	//1/8
		if(PIXMAP_SCALE_SIZE/scale > screenspan)  scale*=2;	//1/16
		int new_width  = width/scale;
		int new_height = height/scale;

		if(new_height > ideal_ch) h = ch;

		int xo = (w/2)-(new_width/2);
		int yo = (h/2)-(new_height/2);
		g.setColor(255,255,255);
		g.fillRect(0, 0, cw, ch);
		g.setColor(200,200,200);
		g.fillRect(xo-2, yo-2, new_width+4, new_height+4);
		g.setColor(128,128,128);
		g.drawRect(xo-2, yo-2, new_width+4, new_height+4);
		for (int x = 0; x < new_width; x++) {
			for (int y = 0; y < new_height; y++) {
				g.setColor(rgb[(y*scale*width)+(x*scale)]);
				g.fillRect(x+xo, y+yo, 1, 1);
			}
		}
	}

	void _drawImage(Graphics g, int size, int cx, int cy){
		if(size == 0) return;
		if(g == null) return;
		if(initialised == false) return;
		if(rgb == null) return;
		if( size >= ((width>height) ? width : height) ){
			if(debug) System.out.println(size + " " + width + " " + height );
			int xo = cx-(width/2);
			int yo = cy-(height/2);
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					g.setColor(rgb[(y*width)+x]);
					g.fillRect(x+xo, y+yo, 1, 1);
				}
			}
		}else{	
			int scale = width > height ?
           	     			(width*100)/size :
							(height*100)/size;
			int new_width  = (width*100)/scale;
			int new_height = (height*100)/scale;
	        if(debug) System.out.println( width + "->" + new_width + " " + 
						height + "->" + new_height + " (" + scale + ")");
			int index = 0;
			int xo = cx-(new_width/2);
			int yo = cy-(new_height/2);
			for (int x = 0; x < new_width; x++) {
				for (int y = 0; y < new_height; y++) {
					index = (((y*scale)/100) * width ) + ((x*scale)/100);
					g.setColor(rgb[index]);
					g.fillRect(x+xo, y+yo, 1, 1);
				}
			}
		
		}
	}
}
