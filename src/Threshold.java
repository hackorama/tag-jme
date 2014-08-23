class Threshold
{
	private Config config = null;
	private Tagimage tagimage = null;
	private int width   = 0;
	private int height  = 0;
	/*CLDC10
	private int scale   = 0; 
	private int span    = 0; 
	CLDC10*/
	//_CLDC11
	private float scale   = 0; 
	private float span    = 0; 
	//CLDC11_
	private int max_rgb = 0;
	private boolean edgemap[] = null;

	//debug only 
	private boolean pixdebug = false;
	private Pixmap dbgpixmap = null;
	//debug only 
	Pixmap pixmap = null;

	Threshold(Config _config, Tagimage _tagimage)
	{
		config = _config;
		tagimage = _tagimage;

		width   = 0;
		height  = 0;
		/*CLDC10
		scale   = 1*100;
		CLDC10*/
		//_CLDC11
		scale   = 1;
		//CLDC11_
		span    = 0;
		max_rgb = 0;
		edgemap = null;

		if(tagimage.isValid()) { 
			resolveScaling();
			max_rgb = tagimage.maxRGB();
		}

		pixdebug = config.CHECK_VISUAL_DEBUG();
		if(pixdebug) dbgpixmap = config.DBGPIXMAP;
		else         dbgpixmap = null;
	}

	void 
	dispose()
	{
	}

	void
	resolveScaling()
	{
		int tag_width  = tagimage.getWidth();
		int tag_height = tagimage.getHeight();
		if( tag_width <= config.THRESHOLD_WINDOW_SIZE || tag_height <= config.THRESHOLD_WINDOW_SIZE ) {
			System.out.println("ASSERT: Too small image size : w=" + tag_width + ", h=" + tag_height );
			if(config.DEBUG) System.exit(1);
		}

		if(config.PIXMAP_NATIVE_SCALE ||
			config.PIXMAP_SCALE_SIZE < config.THRESHOLD_WINDOW_SIZE) {
				width  = tag_width;
				height = tag_height;
		}else{
			/*CLDC10			
			scale  = tag_width > tag_height ? 
				(tag_width*100)/config.PIXMAP_SCALE_SIZE : 
			(tag_height*100)/config.PIXMAP_SCALE_SIZE;
			width  = (tag_width*100)/scale;
			height = (tag_height*100)/scale;
			if(! config.PIXMAP_FAST_SCALE) span = scale/(2*100);
			CLDC10*/
			//_CLDC11
			scale  = tag_width > tag_height ? 
				(float)tag_width/(float)config.PIXMAP_SCALE_SIZE : 
				(float)tag_height/(float)config.PIXMAP_SCALE_SIZE;
			width  = (int)((float)tag_width/scale);
			height = (int)((float)tag_height/scale);
			if(! config.PIXMAP_FAST_SCALE) span = (int)(scale/2.0);
			//CLDC11_
		}

		if(config.DEBUG) System.out.println ( "SCALE: native=" + config.PIXMAP_NATIVE_SCALE 
			+ " fast=" + config.PIXMAP_FAST_SCALE
			+ " scale=" + scale + " span=" + span 
			+ " tag_width=" + tag_width + " tag_height=" + tag_height 
			+ " width=" + width + " height=" + height 
			+ " window=" + config.THRESHOLD_WINDOW_SIZE );

		config.EDGE_MAP = new boolean[width*height]; 
		edgemap = config.EDGE_MAP;
		config.GRID_WIDTH = width;
		config.GRID_HEIGHT = height;
		//tagimage.initPixels(); //JME If resized at Tagimage 
	}

	int
	getPixel(int x, int y)
	{
		/*CLDC10
		if(span < 1) return tagimage.getPixel((x*scale)/100, (y*scale)/100 );	
		int ix = (x*scale)/100;
		int iy = (y*scale)/100;
		CLDC10*/
		//_CLDC11
		if(span < 1) return tagimage.getPixel((int)(x*scale), (int)(y*scale));	
		int ix = (int)(x*scale);
		int iy = (int)(y*scale);
		//CLDC11_

		int pixel = 0, count = 0;
		for(int i=0; i<=span*2; i++) { 
			pixel+=tagimage.getPixel((int)(ix-span+i), iy);
			pixel+=tagimage.getPixel(ix, (int)(iy-span+i));
			count++;
		}
		return (pixel/count);
	}

	void
	debugPixel(int x, int y)
	{
			/*CLDC10
			int r = tagimage.getPixelR((x*scale)/100, (y*scale)/100 );
			int g = tagimage.getPixelG((x*scale)/100, (y*scale)/100 );
			int b = tagimage.getPixelB((x*scale)/100, (y*scale)/100 );
			CLDC10*/
			//_CLDC11
			int r = tagimage.getPixelR((int)(x*scale), (int)(y*scale));
			int g = tagimage.getPixelG((int)(x*scale), (int)(y*scale));
			int b = tagimage.getPixelB((int)(x*scale), (int)(y*scale));
			//CLDC11_
			pixmap.setPixel(x, y, 1, r, g, b);
	}

	void 
	computeEdgemap()
	{
		computeEdgemap(config.THRESHOLD_WINDOW_SIZE,
			config.THRESHOLD_OFFSET * 3 * config.THRESHOLD_RGB_FACTOR);
		if(pixdebug) dbgpixmap.writeImage("threshold");
	}

	/* 
	* Optimised Local Adapaive Thresholding using the MEAN value 
	* of pixels in the local neighborhood (block) as the threshold
	* and binarizes to on or off pixels based on threshold
	* 
	* Applies edge marking also in the same loop, based on the 
	* thresholded on/off pixel values
	* 
	* Blocks are selected rows (width wise) starting from top left 
	* 
	* x,i,width - are in x plane    y,j,height - are in y plane 
	*
	* Neighborhood sums are calculated based on stored deltas from 
	* the last neighborhood on left and last row of blocks above 
	*
	* All border pixels (x < width, y < w, x > width-radius, y > width-radis) 
	* have partial neighborhood block size
	*
	* NOTE: All condition checks are optmised and is order dependent 
	* 
	* TODO : Verify if the diagonal above check in edge marking can be removed 
	* ( check border tracing )
	*/
	void 
	computeEdgemap(int size, int offset)
	{
		int threshold = 0, lastdelta = 0, di = 0;
		boolean thispixel = false, epixel = false; 
		int ex = 0, ey = 0, ei = 0;
		int blocksize = size*size, radius = size/2, half_block = blocksize/2;

		int  td[] = new int[width*height]; //threshold deltas //TODO moving window of (size*width)
		int  ts[] = new int[width];   //threshold sums
		boolean ta[] = new boolean[width*height]; //thresholded pixel on/off array //TODO  moving window of (3*width)


		int y = 0, x = 0;
		for(x = 0; x < (width*height); x++){  td[x] = 0; edgemap[x] = false; }
		for(x = 0; x < width; x++)     ts[x] = 0; 

		for(y = 0; y < height; y++){ 
			for(x = 0; x < width; x++){
				if( y >= (height-radius) ){ //partial: bottom rows
					ts[x] -= td[((y-radius)*width)+x];
					threshold = ts[x] / (size*(radius+height-y)); 
				}else if( y > radius && x == 0 ){ //normal: very first 
					di = ((y+radius)*width) + x;
					for(int i = 0; i < radius; i++) td[di]+=getPixel(i, y+radius); 
					ts[x] += td[di] - td[((y-radius-1)*width)+x];
					threshold = ts[x] /half_block; 
					lastdelta = td[di];
				}else if( y > radius && x >= width - radius ){//normal: partial end 
					di = ((y+radius)*width) + x;
					td[di] = lastdelta - getPixel(x-radius-1,y+radius);
					ts[x] += td[di] - td[((y-radius)*width)+x];
					threshold = ts[x] / ((width-x+radius)*size);
					lastdelta = td[di];
				}else if( y > radius && x <=radius ){ //normal: partial begin 
					di = ((y+radius)*width) + x;
					td[di]=lastdelta + getPixel(x+radius, y+radius);
					ts[x]+= td[di] - td[((y-radius)*width)+x];
					threshold = ts[x] / ((x+radius)*size);
					lastdelta = td[di];
				}else if( y > radius ){ //normal: all full (90% of all)
					di = ((y+radius)*width) + x;
					td[di] = lastdelta + getPixel(x+radius, y+radius) 
						- getPixel(x-radius-1,y+radius);
					ts[x] += td[di] - td[((y-radius)*width)+x];
					threshold = ts[x] / blocksize;
					lastdelta = td[di];
				}else if( x == 0 && y == 0 ){ //first top left block 
					for(int j = 0; j < radius; j++){
						di = j*width;
						for(int i = 0; i < radius; i++) td[di] += getPixel(i, j);
						ts[x] += td[di];
					}
					threshold = ts[x]  /(blocksize/4); 
				}else if( y == 0 ){ //partial: topmost row all
					for(int j = 0; j < radius; j++){
						di = (j*width) + x;
						td[di] = td[di-1];
						if(x > radius)    td[di] -= getPixel(x-radius-1, j);
						if(x <= width-radius) td[di] += getPixel(x+radius, j);
						ts[x] += td[di];
					}
					if(x <= radius) threshold = ts[x] / (radius*(x+radius));
					else if(x <= width-radius) threshold = ts[x] / half_block;
					else            threshold = ts[x] / ((width-x+radius)*radius);
				}else if( y <= radius && x <= radius){ //partial: top row begin
					di = ((y+radius)*width) + x;
					for(int i = 0; i < x+radius; i++) td[di] 
					+= getPixel(i, y+radius); 
					ts[x] += td[di];
					threshold = ts[x] / ((x+radius)*(y+radius)); 
				}else if( y <= radius && x >= width -radius){ //partial: top row end
					di = ((y+radius)*width) + x;
					for(int i = width; i > x-radius; i--) td[di] 
					+= getPixel(i, y+radius);
					ts[x] += td[di];
					threshold = ts[x] / ((width-x+radius)*(y+radius)); 
				}else if( y <= radius ){ //partial: top row all
					di = ((y+radius)*width) + x;
					for(int i = 0; i < size; i++) td[di] 
					+= getPixel((x-radius)+i, y+radius); 
					ts[x] += td[di];
					threshold = ts[x] / (size*(y+radius)); 
				}
				threshold-=offset;
				thispixel = (getPixel(x, y) < threshold) ? true : false;
				ta[(y*width)+x] = thispixel;
				//JAVE_PORT if(pixdebug) thispixel ? d_setPixelFilled(x, y) : d_setPixelBlank(x, y);
				/*if(pixdebug) { 
				if(thispixel) d_setPixelFilled(x, y); 
				else          d_setPixelBlank(x, y); 
				}*/
				if( y > 2){ //edge marking based on ta[]
					ex = x; ey = y-2;
					ei = ((ey)*width)+ex;
					epixel = ta[ei];
					if( epixel ){
						if( ex > 0 && ex < width ){
							if( epixel ^ ta[(ey*width)+ex-1]
							|| epixel ^ ta[(ey*width)+ex+1]
							|| epixel ^ ta[((ey-1)*width)+ex]
							|| epixel ^ ta[((ey-1)*width)+ex+1]
							|| epixel ^ ta[((ey-1)*width)+ex-1]
							|| epixel ^ ta[((ey+1)*width)+ex]
							|| epixel ^ ta[((ey+1)*width)+ex+1]
							|| epixel ^ ta[((ey+1)*width)+ex-1] ){
								edgemap[ei] = true;		
								if(pixdebug) d_setPixelMarked(ex, ey);	
							}
						}
					}
				}
				//close out borders on bottom edge
				//if((y == height-1) && thispixel) d_setPixelMarked(x, y); 
			}
			//close out borders on right edge
			//if((x == width) && thispixel) d_setPixelMarked(x-1, y); 
		}
		ta = null;
		ts = null;
		td = null;
		if(pixdebug) pixmap.mapEdges(edgemap, width, height);
	}

	void
	d_setPixelMarked(int x, int y) //GREEN
	{
		dbgpixmap.setPixel(x, y, 0, max_rgb, 0);	
	}

	void
	d_setPixelBlank(int x, int y) //WHITE 
	{
		dbgpixmap.setPixel(x, y, max_rgb, 0, 0);	
	}

	void
	d_setPixelFilled(int x, int y) //BLACK
	{
		dbgpixmap.setPixel(x, y, 0, 0, max_rgb);	
	}

}
