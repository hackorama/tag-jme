import java.util.Vector;
import java.lang.Math.*;

class Shape
{

	private Config config;
	private int xmap[];	
	private int ymap[];	
	private int  mapcount;
	private int widths_at_y[];
	private int heights_at_x[];
	private int midpoints_at_y[];
	private int midpoints_at_x[];
	private boolean rotated; 
	private int  width, height;
	private int  min_x, max_x, min_y, max_y;
	private int  center_x, center_y;
	private int  grid_w, grid_h; 
	private int  midpoint;

	//debug only
	private boolean debug;
	private boolean pixdebug;
	private boolean anchordebug;
	private Pixmap d_pixmap; 


	Shape()
	{
		config = null;
		init();
	}

	Shape(Config _config)
	{
		config = _config;
		init();
	}

	void
	test()
	{
		System.out.println("Shape OK");
	}

	void
	dispose()
	{
		if(widths_at_y != null)    widths_at_y = null;
		if(heights_at_x != null)   heights_at_x = null;
		if(midpoints_at_y != null) midpoints_at_y = null;
		if(midpoints_at_x != null) midpoints_at_x = null;
		if(xmap != null)           xmap = null;
		if(ymap != null)           ymap = null;
	}

	void
	init()
	{
		width  = 0;  height = 0;
		grid_w = 0; grid_h = 0;  // grid size = pixmap size
		center_x = 0; center_y = 0; 
		min_x = 0; max_x = 0;
		min_y = 0; max_y = 0;

		xmap = null;
		ymap = null;
		mapcount = 0;

		rotated  = false;  
		d_pixmap = null;
		debug    = false;
		pixdebug = false;
		anchordebug = false;
		widths_at_y = null;
		heights_at_x = null;
		midpoints_at_y = null;
		midpoints_at_x = null;
		if(config != null){
			debug       = config.DEBUG;
			pixdebug    = config.CHECK_VISUAL_DEBUG();
			anchordebug = config.ANCHOR_DEBUG;
			grid_w = config.GRID_WIDTH;
			grid_h = config.GRID_HEIGHT;
		}
		//global to hold mid value in  widthAt() call  - FIXME
		midpoint = 0;
	}

	//CPPONLY Only the default constructor used for array of objects
	void
	setConfig(Config _config) 
	{
		config = _config;
		debug       = config.DEBUG;
		pixdebug    = config.CHECK_VISUAL_DEBUG();
		anchordebug = config.ANCHOR_DEBUG;
		grid_w = config.GRID_WIDTH;
		grid_h = config.GRID_HEIGHT;
	}

	void 
	setBounds(int _min_x, int _min_y, int _max_x, int _max_y)
	{
		min_x = _min_x;
		min_y = _min_y;
		max_x = _max_x;
		max_y = _max_y;

		width = max_x - min_x ;
		height = max_y - min_y ;
	}

	void 
	setGrid(int _w, int _h)
	{
		grid_w = _w;
		grid_h = _h;
	}

	void 
	setCenter(int x, int y)
	{
		center_x = x;
		center_y = y;
	}

	void 
	setValues(Vector _xmap, Vector _ymap, int _mapcount)
	{
		mapcount = _mapcount;

		if( mapcount != (int) _xmap.size() ) {
			System.err.println("ASSERT: mapcount mismatch" ); 
			if(debug) System.exit(1); 
		}
		if( mapcount != (int) _ymap.size() ) {
			System.err.println("ASSERT: mapcount mismatch" ); 
			if(debug) System.exit(1);
		}

		if(xmap != null) xmap = null;
		if(ymap != null) ymap = null;
		xmap = new int[mapcount];
		ymap = new int[mapcount];

		for(int i=0; i< mapcount; i++) xmap[i] = ((Integer) _xmap.elementAt(i)).intValue();
		for(int i=0; i< mapcount; i++) ymap[i] = ((Integer) _ymap.elementAt(i)).intValue();
	}

	void 
	copyValues(int _xmap[], int _ymap[], int _mapcount)
	{
		mapcount = _mapcount;
		if(xmap != null) xmap = null;
		if(ymap != null) ymap = null;
		xmap = new int[mapcount];
		ymap = new int[mapcount];

		int i = 0;
		while(i < mapcount){
			xmap[i] = _xmap[i];
			ymap[i] = _ymap[i];
			i++;
		}
	}

	int[]
	getWidthValues()
	{
		return widths_at_y;
	}

	int[]
	getHeightValues()
	{
		return heights_at_x;
	}

	int[]
	getHMidpointValues()
	{
		return midpoints_at_x;
	}

	int[]
	getWMidpointValues()
	{
		return midpoints_at_y;
	}

	void 
	copyHeightValues(int heights[], int mids[], int min, int max)
	{
		if( heights_at_x != null ) { heights_at_x = null;}
		if( midpoints_at_x != null ) { midpoints_at_x = null;}
		heights_at_x = new int[max-min];
		midpoints_at_x = new int[max-min];

		int c = 0;
		for(int i=min; i<max; i++) { 
			heights_at_x[c]   = heights[c]; 
			midpoints_at_x[c] = mids[c];
			c++; 
		}
	}

	void 
	copyWidthValues(int widths[], int mids[], int min, int max)
	{
		if( widths_at_y != null ) { widths_at_y = null; }
		if( midpoints_at_y != null ) {  midpoints_at_y = null; }
		widths_at_y = new int[max-min];
		midpoints_at_y = new int[max-min];

		int c = 0;
		for(int i=min; i<max; i++) { 
			widths_at_y[c]    = widths[c]; 
			midpoints_at_y[c] = mids[c];
			c++; 
		}
	}

	void 
	setHeightValues(int heights_holder[], int mids_holder[], int min, int max, boolean reset)
	{
		if( heights_at_x != null ) { heights_at_x = null;}
		if( midpoints_at_x != null ) { midpoints_at_x = null;}
		heights_at_x = new int[max-min];
		midpoints_at_x = new int[max-min];

		int c = 0;
		for(int i=min; i<max; i++) {
			int h = heights_holder[i];
			heights_at_x[c] = h > 0 ? h : 0;
			midpoints_at_x[c] = mids_holder[i];
			//if(reset) heights_holder[i] = 0;
			//if(reset) mids_holder[i] = 0;
			c++;
		}
	}

	void 
	setWidthValues(int widths_holder[], int mids_holder[], int min, int max, boolean reset)
	{
		if( widths_at_y != null ) { widths_at_y = null; }
		if( midpoints_at_y != null ) { midpoints_at_y = null; }
		widths_at_y = new int[max-min];
		midpoints_at_y = new int[max-min];

		int c = 0;
		for(int i=min; i<max; i++) {
			int w = widths_holder[i];
			widths_at_y[c] = w > 0 ? w : 0;
			midpoints_at_y[c] = mids_holder[i];
			//if(reset) widths_holder[i] = 0;
			//if(reset) mids_holder[i] = 0;
			c++;
		}
	}

	void
	addWidths(int widths_holder[], int wmids_holder[], int x, int y)
	{
		if(widths_holder[y] == 0){
			widths_holder[y]  = x*-1;
			wmids_holder[y] = x;
		}else if(widths_holder[y] < 0){
			int diff = Math.abs(widths_holder[y]+x);
			if(diff > 4)  { 
				int lastx = Math.abs(widths_holder[y]);
				wmids_holder[y] = x > lastx ? lastx+(diff/2) : x+(diff/2);
				widths_holder[y]  = diff;
			}
		}
	}

	void
	addHeights(int heights_holder[], int hmids_holder[], int x, int y)
	{
		if(heights_holder[x] == 0){
			heights_holder[x] = y*=-1;
			hmids_holder[x] = y;
		}else if(heights_holder[x] < 0){
			int diff = Math.abs(heights_holder[x]+y);
			if(diff > 4) { 
				int lasty = Math.abs(heights_holder[x]);
				hmids_holder[x] = y > lasty ? lasty+(diff/2) : y+(diff/2);
				heights_holder[x]  = diff;
			}
		}
	}


	void
	d_printWidths()
	{
		int l = max_y - min_y;
		int s = 0, c = 0;
		System.out.println( "W :" );
		for(int i=0; i<l; i++) { 
			if(widths_at_y[i] > 0){
				s+=widths_at_y[i];
				c++;
			}
			System.out.println( widths_at_y[i] + "|" + midpoints_at_y[i] + " " );
		}
		int ave = c > 0 ? s/c : 0 ;
		if(debug) System.out.println( ": W count=" + l  + " ave=" + ave + " (nonzero=" + c + ")" ); 
	}

	void
	d_printHeights()
	{
		int l = max_x - min_x;
		int s = 0, c = 0;
		System.out.println( "H :" );
		for(int i=0; i<l; i++) { 
			if(heights_at_x[i] > 0 ){
				s+=heights_at_x[i];
				c++;
			}
			System.out.println( heights_at_x[i] + "|" +  midpoints_at_x[i] + " " );
		}
		int ave = c > 0 ? s/c : 0 ;
		if(debug) System.out.println( ": H count=" + l  + " ave=" + ave + " (nonzero=" + c + ")" ); 
	}

	boolean
	isWithin(int x, int y, int s)
	{
		return isWithin( x, y, s, center_x, center_y);
	}

	boolean
	isWithin(int x, int y, int sx, int sy)
	{
		return isWithin( x, y, sx, sy, center_x, center_y);
	}

	boolean
	isWithin(int x, int y, int s, int ax, int ay)
	{
		if( ax > x && ay > y &&  ax < (x+s) && ay < (y+s) ) return true;
		return false;
	}

	boolean
	isWithin(int x, int y, int sx, int sy, int ax, int ay)
	{
		if( ax > x && ay > y &&  ax < (x+sx) && ay < (y+sy) ) return true;
		return false;
	}

	boolean
	isRotated()
	{
		return rotated;
	}

	void
	d_setPixmap(Pixmap _d_pixmap)
	{
		if(pixdebug) d_pixmap = _d_pixmap;	
	}

	void
	add( int x, int y, int mapcount)
	{
		xmap[mapcount] = x;
		ymap[mapcount] = y;
	}

	int
	getWidth()
	{
		return width;
	}

	int
	getHeight()
	{
		return height;
	}

	int
	length()
	{
		return mapcount;
	}

	int
	getgridw()
	{
		return grid_w;
	}

	int
	getgridh()
	{
		return grid_h;
	}

	int
	size()
	{
		return (width*height);
	}

	void
	rotateShape(int d)
	{
		if( d == 0 ) return;
		rotateShape(center_x, center_y, 0, 0, d);
	}

	void
	rotateShape(int x, int y, int dx, int dy, int d)
	{
		rotated = true;
		if( d == 0 ) return;
		//if( d < 0 ) d =  360 - d; //angle correction 
		d = d * -1 ;

		grid_w = config.GRID_WIDTH;
		grid_h = config.GRID_HEIGHT;

		int x1 = 0, y1 = 0;
		int x2 = 0, y2 = 0;
		int n_min_x = 99999999, n_min_y = 99999999, n_max_x = 0, n_max_y = 0;
		
		/*CLDC10
		int a3 = 0, a4 = 0;
		int a = ( (3141 * d) / 180000 );
		int a1 = IntMath.cos(a);
		int a2 = IntMath.sin(a);
		CLDC10*/		
		//_CLDC11
		double a3 = 0.0, a4 =0.0;
                double a = (double) ( ((double)3.1415926535897931 * (double)d) / (double)180 );
                double a1 = Math.cos(a);
                double a2 = Math.sin(a);
		//CLDC11_

		int widths_holder[] = new int[grid_h];
		int heights_holder[] = new int[grid_w];
		for(int i=0; i<grid_h; i++) widths_holder[i] = 0;
		for(int i=0; i<grid_w; i++) heights_holder[i] = 0;
		int wmids_holder[] = new int[grid_h];
		int hmids_holder[] = new int[grid_w];
		for(int i=0; i<grid_h; i++) wmids_holder[i] = 0;
		for(int i=0; i<grid_w; i++) hmids_holder[i] = 0;

		int i = 0;
		while( i < mapcount ) {

			x1 = xmap[i] + dx ;
			y1 = ymap[i] + dy ;

			/*CLDC10
			a3 = (x1 - x) * a1 + (y1 - y) * a2 + x;
			a4 = (-(x1 - x)) * a2 + (y1 - y) * a1 + y;

			x2 = a3;
			y2 = a4;
			CLDC10*/
			//_CLDC11
			a3 = (double)(x1 - x) * a1 + (double)(y1 - y) * a2 + (double)x;
            a4 = (double)(-(x1 - x)) * a2 + (double)(y1 - y) * a1 + (double)y;

            x2 = (int)a3;
            y2 = (int)a4;
			//CLDC11_

			if(debug){ 
				if( x2 < 0 || x2 > grid_w ) System.out.println( "E x" + x1 + "." + x2 );
				if( y2 < 0 || y2 > grid_h ) System.out.println( "E y" + y1 + "." + y2 );
			}

			//FIXME bounds check
			if(x2 < 0) x2 = 0;
			if(y2 < 0) y2 = 0;
			if(x2 >= grid_w) x2 = grid_w-1;
			if(y2 >= grid_h) y2 = grid_h-1;

			xmap[i] = x2;
			ymap[i] = y2;

			addWidths(widths_holder, wmids_holder, x2, y2);
			addHeights(heights_holder, hmids_holder, x2, y2);

			i++;

			if(x2 > n_max_x) n_max_x = x2;
			if(x2 < n_min_x) n_min_x = x2;
			if(y2 > n_max_y) n_max_y = y2;
			if(y2 < n_min_y) n_min_y = y2;
		}

		setWidthValues(widths_holder, wmids_holder, n_min_y, n_max_y, true);
		setHeightValues(heights_holder, hmids_holder, n_min_x, n_max_x, true);
		widths_holder = null;
		heights_holder = null;
		wmids_holder = null;
		hmids_holder = null;
		setBounds(n_min_x, n_min_y, n_max_x, n_max_y);
		setCenter(min_x + (max_x - min_x)/2,  min_y + (max_y - min_y)/2);

	}

	int
	matchPattern()
	{
		int result = matchBars();
		if( result == -1 )  result =  matchBox();
		if(debug) { 
			if(result == -1) System.out.println(" F");
			else             System.out.println(" OK");  
		}
		//DPIX if(pixdebug) d_pixmap.markPoint( center_x, center_y, 2);
		return result;
	}

	/*  
	* +----------------+
	* |   3   |    2   |
	* +----------------+
	* |  -|-  |   # #  |
	* |  # #  |   -|-  | 
	* +----------------+
	* 
	* w  - width of shape to match
	* h  - width of shape to match
	* 
	* hd - height delta - 1/4 th of height
	* 
	* center_x - shape center - middle of max_x min_x
	* center_y - shape center - middle of max_y min_y
	*
	* tw - top width 	width 1/4 the from top 
	* bw - bottom width 	width 1/4 the from bottom 
	*
	* tm - top middle 	center at 1/4 the from top 
	* bw - bottom midle 	center at 1/4 the from bottom
	* 
	*/
	int
	matchBars()
	{
		boolean TOP_L = false;
		boolean BOT_L = false;
		boolean TOP_R = false;
		boolean BOT_R = false;

		int w = max_x - min_x;
		int h = max_y - min_y;
		int hd = h / 4;


		if(debug) System.out.println( "[matchBar" );

		int tw = widthAt(min_y + hd, 4);
		if( tw < 0 ) return -1; //FIXEME 
		int tm = midpoint;
		if( ! isEqual(tw, w) ) {	 //only if not very wide 
			if(isEqual(center_x, tm) ){ 	 //and is at middle
				if ( tw <= w/3 ){//and is thin
					TOP_R = true;
					TOP_L = true;
				}
			}
		}

		int bw = widthAt(max_y - hd, 4);
		if( bw < 0 ) return -1; //FIXEME 
		int bm = midpoint;
		if( ! isEqual(bw, w) ) {	  //only if not wide 
			if(isEqual(center_x, bm) ){  	  //and is at middle	
				if ( bw <= w/3 ){ //and is thin
					BOT_R = true;
					BOT_L = true;
				}
			}
		}

		d_debugMatchBars(tw, tm, bw, bm);
		if(debug) System.out.println( "matchBar]" );

		if( TOP_L && TOP_R ) return 3;
		if( BOT_L && BOT_R ) return 2;
		return -1;

	}

	/*  
	* +----------------+
	* |   3   |    2   |
	* +----------------+
	* |  -|-  |   # #  |
	* |  # #  |   -|-  | 
	* +----------------+
	* 
	* +---------------+-------+-------+-------+-------+-------+-------+
	* |   6   |   7   |   4   |   5   |   9   |   0   |   8   |   1   |
	* +---------------+-------+-------+-------+-------+-------+-------+
	* |  # -  |  - #  |  # -  |  - #  |  # -  |  - #  |  - -  |  - -  |
	* |  - #  |  # -  |  # -  |  - #  |  - -  |  - -  |  - #  |  # -  | 
	* +---------------+-------+-------+-------+-------+-------+-------+
	* 
	* w  - width of shape to match
	* h  - width of shape to match
	* 
	* hd - height delta - 1/4 th of height
	* 
	* center_x - shape center - middle of max_x min_x
	* center_y - shape center - middle of max_y min_y
	*
	* tw - top width 	width 1/4 the from top 
	* bw - bottom width 	width 1/4 the from bottom 
	*
	*/
	int
	matchBox()
	{
		boolean TOP_L = false;
		boolean BOT_L = false;
		boolean TOP_R = false;
		boolean BOT_R = false;

		int w = max_x - min_x;
		int h = max_y - min_y;
		int hd = h / 4;

		int tw = widthAt(min_y + hd, 4);
		if( tw < 0 ) return -1; //FIXEME 
		int tm = midpoint;
		if( ! isEqual(tw, w)) {		//if top not full wide as shape
			if(tm > center_x ) TOP_R = true;	//if top center on right of shape center
			else   	     TOP_L = true;	//if top center on left of shape center
		}

		int bw = widthAt(max_y - hd, 4);
		if( bw < 0 ) return -1; //FIXEME 
		int bm = midpoint;
		if( ! isEqual(bw, w) ) {		//if bottom not full wide as ahape
			if(bm > center_x ) BOT_R = true;	//if bottom center on right of shape center
			else	     BOT_L = true;	//if bottom center on left of shape center
		}

		d_debugMatchBox(w, tw, tm, bw, bm);

		if( TOP_L && BOT_R ) return 6;
		if( TOP_R && BOT_L ) return 7;
		if( TOP_L && BOT_L ) return 4;
		if( TOP_R && BOT_R ) return 5;
		if( BOT_R ) return 8;
		if( TOP_L ) return 9;
		if( TOP_R ) return 0;
		if( BOT_L ) return 1;
		return -1;
	}


	boolean
	isAnchor()
	{
		if(  width == 0 || height == 0  ) return false;

		if(! isAnchorLike()) return false;

		int bigger = width > height ? width : height;
		//int flex = (int)(((float)bigger * (float)config.ANCHOR_BOX_FLEX_PERCENT )/100);
		int flex = (bigger * config.ANCHOR_BOX_FLEX_PERCENT)/100; //JFLOAT

		if(anchordebug) System.out.println( length() + " " + width + " " + height + " : " 
			+ widths_at_y[height/2] + "-" + heights_at_x[width/2] + " "
			+ widths_at_y[height/4] + "-" + widths_at_y[height-(height/4)] + " "
			+ heights_at_x[width/4] + "-" + heights_at_x[width-(width/4)] + "(" + flex + ")" );

		/*if(anchordebug){
		for(int i=0; i<width; i++) { 
		System.out.println( " " + heights_at_x[i] ); 
		if( i == width/4) System.out.println( );
		if( i == width/2) System.out.println( );
		if( i == (width-(width/4)) ) System.out.println( );
		}
		System.out.println( );
		for(int i=0; i<height; i++) { 
		System.out.println( " " + widths_at_y[i] ); 
		if( i == height/4) System.out.println( );
		if( i == height/2) System.out.println( );
		if( i == (height-(height/4)) ) System.out.println( );
		}
		}*/
		if( Math.abs(width-height) > flex ) return false;
		if ( Math.abs(widths_at_y[height/2]-heights_at_x[width/2]) > flex)      return false;
		if ( Math.abs(widths_at_y[height/4]-widths_at_y[height-(height/4)]) > flex)   return false;
		if ( Math.abs(heights_at_x[width/4]-heights_at_x[width-(width/4)]) > flex) return false;
		if ( Math.abs(widths_at_y[height/2]-width)  > (2*flex)) return false;
		if ( Math.abs(heights_at_x[width/2]-height) > (2*flex)) return false;
		return true;
	}

	boolean
	isAnchorLike()
	{
		int l = length();
		if(l == 0) return false;
		int upperlimit = (grid_w > grid_h) ?  (grid_w * 3) : (grid_h * 3) ;
		int lowerlimit = upperlimit / 8;
		if(anchordebug) System.out.print( l + " " + upperlimit + " " + lowerlimit );
		if(anchordebug) System.out.println( "isAnchorLike : limit=" + upperlimit + " length=" + l );
		if(l > upperlimit) return false;
		if(l < lowerlimit) return false;
		if(anchordebug) System.out.println( "isAnchorLike : TRUE "  );
		return true;
	}

	/*
	* findTilt() : Angle of tilt calculated with distance 
	*              from corners (max/min_x/y)
	*
	* findDirection() : Direction of tilt calculated with 
	*                   distance from side mid points (max/min_x/y, center_x/y)
	* 
	*   min_x, min_y       center_x,min_y      max_x, min_y
	*               +---#-----+---------+
	*               |         |         |
	*               |  x2,y2  |  x1,y1  #
	*               |         |         |
	*      min_x,center_y +---------+---------+ max_x,center_y
	*               |         |         |
	*               #  x3,y3  |  x4,y4  |
	*               |         |         |
	*               +---------+-----#---+
	*   min_x, max_y      center_x,max_y       max_x, max_y
	* 
	*
	*   # - tilted anchor corner points ( x1/2/3/4, y1/2/3/4 )
	*
	*/
	int
	findTilt()
	{
		int x1 = center_x, y1 = center_y, p1 = grid_w * grid_h;
		int x2 = center_x, y2 = center_y, p2 = grid_w * grid_h;
		int x3 = center_x, y3 = center_y, p3 = grid_w * grid_h;
		int x4 = center_x, y4 = center_y, p4 = grid_w * grid_h;
		int angle = 0, a1=0, a2=0, a3=0, a4=0;
		int x=0, y=0, p=0; // p : roximity to corner
		int bbx = (max_x - min_x)/3, bby = (max_y - min_y)/3; //inner bounding box
		int direction = 1;

		/*if(debug){
			System.out.println( "[W " );
			int c = max_y-min_y;
			for(int i = 1; i < c/2; i++){
				System.out.println( widths_at_y[i] + "~" + widths_at_y[c-i] + " " );
			}
			System.out.println( "W] " );
			System.out.println( "[H " );
			c = max_x-min_x;
			for(int i = 1; i < c/2; i++){
				System.out.println( heights_at_x[i] + "~" + heights_at_x[c-i] + " " );
			}
			System.out.println( "H] " );
		}*/

		/* 
		* loop through all border pixels
		* only select pixels close to bounding box edge 
		* (selected by outside inner bounding box)
		* for each quandrant measure disance from coner
		* closeset to corner for each quandrant is picked as 
		* the anchor corner point
		*/
		int i = 0;	
		while( i < mapcount ){
			x = xmap[i];
			y = ymap[i];
			if( Math.abs(x-center_x) > bbx || Math.abs(y-center_y) > bby){//close to edge
				if( x > center_x  && y <= center_y ){	//top right Q1
					p = (max_x - x) + (y - min_y); 
					if(p < p1){
						p1 = p; x1 = x; y1 = y;
					}
				}else if( x <= center_x && y <= center_y ){	//top left  Q2
					p = (x - min_x) + (y - min_y); 
					if(p < p2){
						p2 = p; x2 = x; y2 = y;
					}
				}else if( x <= center_x  && y > center_y ){	//bot left  Q3
					p = (x - min_x) + (max_y - y); 
					if(p < p3){
						p3 = p; x3 = x; y3 = y;
					}
				}else if( x > center_x  && y > center_y ){	//bot right Q4
					p = (max_x - x) + (max_y - y); 
					if(p < p4){
						p4 = p; x4 = x; y4 = y;
					}
				}
			}
			i++;
		}
		a1 = findAngle(x2, y2, x1, y1);
		a2 = findAngle(x1, y1, x4, y4);
		a3 = findAngle(x4, y4, x3, y3);
		a4 = findAngle(x3, y3, x2, y2);

		angle = isDiagonal(a1, a2, a3, a4) ? maxAngle(a1, a2, a3, a4) : (a1+a2+a3+a4)/4;
		if(angle != 45 ) direction = findDirection(x1, y1, x2, y2, x3, y3, x4, y4);
		angle*=direction;

		if(debug) System.out.println( "Shape.findTilt [ " + angle + " | " + a1 + " " + a2 + " " + a3 + " " + a4 + " ]"  );
		if( pixdebug ){
			d_pixmap.clearPixmap();
			d_markAnchor();
			d_pixmap.setPen(0, 255, 0);
			d_pixmap.markPoint( x1, y1, 6);
			d_pixmap.setPen(0, 0, 255);
			d_pixmap.markPoint( x4, y4, 6);
			d_pixmap.setPen(0, 255, 255);
			d_pixmap.markPoint( x3, y3, 6);
			d_pixmap.setPen(0, 0, 0);
			d_pixmap.markPoint( x2, y2, 6);
			d_pixmap.writeImage("corners");
		}

		return angle;
	}

	int 
	findDirection(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) //order dependent 
	{
		int reverse = 0;
		if (pointProximity(center_x, min_y, x1, y1) > pointProximity(max_x, center_y, x1, y1)) reverse++;
		if (pointProximity(min_x, center_y, x2, y2) > pointProximity(center_x, min_y, x2, y2)) reverse++;
		if (pointProximity(center_x, max_y, x3, y3) > pointProximity(max_x, center_y, x3, y3)) reverse++;
		if (pointProximity(max_x, center_y, x4, y4) > pointProximity(center_x, max_y, x4, y4)) reverse++;

		if (reverse > 2) return -1;
		return 1;
	}

	//special case check for 45 degree diagonal 
	boolean 
	isDiagonal(int a1, int a2, int a3, int a4)  
	{
		if( a1 > 40 || a2 > 40 || a3 > 40 || a4 > 40) return true;
		/* if( a1 == 0 || a2 == 0 || a3 == 0 || a4 == 0) //possible 45 degree diagonal
		if( a1 > 25 || a2 > 25 || a3 > 25 || a4 > 25 )  //certain 45 degree diagonal
		return true;
		*/
		return false;
	}

	int 
	maxAngle(int a1, int a2, int a3, int a4)  
	{
		int max = a1;
		if( a2 > max ) max = a2; 
		if( a3 > max ) max = a3; 
		if( a4 > max ) max = a4; 
		return max;
	}

	int 
	minAngle(int a1, int a2, int a3, int a4)  
	{
		int min = a1;
		if( a2 < min ) min = a2; 
		if( a3 < min ) min = a3; 
		if( a4 < min ) min = a4; 
		return min;
	}

	int 
	pointProximity(int x1, int y1, int x2, int y2)  
	{
		return (Math.abs(x1-x2) + Math.abs(y1-y2));
	}

	/*CLDC10	
	int 
	findAngle(int x1, int y1, int x2, int y2)  
	{
		int o = IntMath.abs(x1 - x2);
		int a = IntMath.abs(y2 - y1);
		if( o <= 0 || a <= 0 ) return 0;
		int angle = (o < a) ? IntMath.tan(o/a) * 180000 / 3141  :
			IntMath.tan(a/o) * 180000 / 3141  ;
		//angle = angle > 90 ? 90 - angle : angle;
		return angle;
	}
	CLDC10*/
	//_CLDC11
	int
    findAngle(int x1, int y1, int x2, int y2)
    {
        double o = Math.abs(x1 - x2);
        double a = Math.abs(y2 - y1);
        if( o <= 0 || a <= 0 ) return 0;
        double angle = (o < a) ? Math.tan(o/a) * 180 / 3.14159265  :
            Math.tan(a/o) * 180 / 3.14159265  ;
        //angle = angle > 90 ? 90 - angle : angle;
        return (int)angle;
    }
	//CLDC11_

	int
	heightAt(int x) //unused now, future use
	{
		int nx = x-min_x;
		return heights_at_x[nx];
	}

	int
	widthAt(int y)
	{
		try{
			int ny = y-min_y;
			midpoint = midpoints_at_y[ny];
			//DPIX if(pixdebug) d_pixmap.markPoint( midpoint, y, 2);
			return widths_at_y[ny];
		}catch(Exception e){ //FIXME
			if(debug) System.out.println("\n***\n WIDTHAT : " + e.getMessage() );  
			return -1; 
		}
	}

	// rotated shapes have broken borders retry width and mid  - FIXME (Pattern)
	// shapes may have adjacent pixels as widthAt == 1 TODO: verify w<=1  or w<1
	int
	widthAt(int y, int delta)
	{
		try{
			int w  = widthAt(y);
			int ny = y - (delta/2); 
			while( w <= 1 && ny < y+delta ) w = widthAt(ny++); 
			return w;
		}catch(Exception e){ //FIXME
			if(debug) System.out.println("\n***\nWIDTHAT DELTA: " + e.getMessage() ); 
			return -1; 
		}
	}

	boolean
	isEqualByPixelThreshold(int a, int b, int threshold)
	{
		if( a == b ) return true;
		if( threshold == 0  ) return false;
		if( Math.abs(a-b) <= threshold ) return true;
		return false;
	}

	boolean
	isEqual(int a, int b)
	{
		if(a == b) return true;
		int threshold = config.SHAPE_BOX_FLEX_PERCENT;
		int bigger = a > b ? a : b;
		if(debug) System.out.println( a + " " + b + " " + 
				bigger * threshold + "/100] "  ); 
		//if( Math.abs(a-b) < ((float)bigger * ((float)threshold / 100)) ) return true; //JFLOAT
		if( (Math.abs(a-b)*100) < (bigger * threshold)) return true; //JFLOAT
		return false;
	}

	int
	getx(int y)
	{
		int x = 0;
		for( int i = 0; i < mapcount; i++){
			if( ymap[i] == y ) { 
				if( xmap[i] > x ) x = xmap[i];
			}
		}
		return x;
	}

	int
	gety(int x)
	{
		int y = 0;
		for( int i = 0; i < mapcount; i++){
			if( xmap[i] == x ) { 
				if( ymap[i] > y ) y = ymap[i];
			}
		}
		return y;
	}

	int[]
	getxmap()
	{
		return xmap;
	}

	int[]
	getymap()
	{
		return ymap;
	}

	int
	getmapcount()
	{
		return mapcount;
	}

	int
	getcx()
	{
		return center_x;
	}

	int
	getcy()
	{
		return center_y;
	}

	int
	getminx()
	{
		return min_x;
	}

	int
	getmaxx()
	{
		return max_x;
	}

	int
	getminy()
	{
		return min_y;
	}

	int
	getmaxy()
	{
		return max_y;
	}

	//DEBUG ROUTINES

	void
	d_debugMatchBars(int tw, int tm, int bw, int bm)
	{
		if( ! debug && ! pixdebug ) return;
		if(debug){ 
			/* PORTME System.out.println( " [bar w=" + width + " tw=" + tw + " bw=" + bw + " center_x=" + 
			center_x + " tm=" + tm + " bm=" + bm + "] " ); */
		}
		if(pixdebug){
			if( tw > 0 ){
				//DPIX d_pixmap.savePen();;
				//DPIX d_pixmap.setPen(255, 140, 0); //ORANGE
				//DPIX d_pixmap.markHLine( min_x, min_x + tw, min_y + (height/4) );
				//DPIX d_pixmap.setPen(0, 190, 255); //SKYBLUE
				//DPIX d_pixmap.markVLine( min_y, min_y + (height/4), tm );
				//DPIX d_pixmap.restorePen();;
			}
			if( bw > 0 ){
				//DPIX d_pixmap.savePen();;
				//DPIX d_pixmap.setPen(255, 140, 0); //ORANGE
				//DPIX d_pixmap.markHLine( min_x, min_x + bw, max_y - (height/4) );
				//DPIX d_pixmap.setPen(0, 190, 255); //SKYBLUE
				//DPIX d_pixmap.markVLine( max_y - (height/4) , max_y, bm );
				//DPIX d_pixmap.restorePen();;
			}
		}
	}

	void
	d_debugMatchBox(int w, int tw, int tm, int bw, int bm)
	{
		if( ! debug && ! pixdebug ) return;
		if(debug){ 
			/* PORTME System.out.println( " [box w=" + w + " tw=" + tw + " bw=" + bw + " center_x=" + 
			center_x + " tm=" + tm + " bm=" + bm + "] " );*/
			if(!isEqual(tw, w/2) ) {	
				System.out.println( " WARN: matchBox " );
				System.out.println( " top midx = " + tm + " >  center_x=" + center_x );
				System.out.println( " w = " + w + " tw = " + tw );
			}
			if ( ! isEqual(bw, w/2) ) {
				System.out.println( "WARN: matchBox" );
				System.out.println( " bot midx = " + bm + " >  center_x=" + center_x );
				System.out.println( " w = " + w + " bw = " + bw );
			}
		}
		if(pixdebug){
			if( tw > 0 ){
				//DPIX d_pixmap.savePen();;
				//DPIX d_pixmap.setPen(0, 0, 100); //DARKBLUE
				//DPIX d_pixmap.markHLine( min_x, min_x + tw, min_y+(height/4) );
				//DPIX d_pixmap.restorePen();;
			}
			if( tm > 0){
				//DPIX d_pixmap.savePen();;
				//DPIX d_pixmap.setPen(255, 0, 255); //PINK
				//DPIX d_pixmap.markVLine( min_y, min_y+(height/4), tm );
				//DPIX d_pixmap.restorePen();;
			}
			if(bw > 0){
				//DPIX d_pixmap.savePen();;
				//DPIX d_pixmap.setPen(139, 35, 35);//BROWN
				//DPIX d_pixmap.markHLine( min_x, min_x + bw, max_y-(height/4) );
				//DPIX d_pixmap.restorePen();;
			}
			if(bm > 0){
				//DPIX d_pixmap.savePen();
				//DPIX d_pixmap.setPen(139, 0, 139); //VIOLET
				//DPIX d_pixmap.markVLine( max_y-(height/4) , max_y, bm );
				//DPIX d_pixmap.restorePen();;
			}
		}
	}

	void
	d_marklinex(int x1, int x2, int y)
	{
		//DPIX if(pixdebug) d_pixmap.markHLine( x1, x2, y);
	}

	void
	d_markliney(int y1, int y2, int x)
	{
		//DPIX if(pixdebug) d_pixmap.markVLine( y1, y2, x);
	}

	void
	d_markAnchorTilt()
	{
		if(!pixdebug) return;

		int x1 = getmaxx();
		int y1 = gety(x1);
		// tilt check with maxx pixel offset to the center pixel using y 
		int y2 = (y1 > getcy()) ? getminy() : getmaxy();
		int x2 = getx(y2);

		int o = x1 - x2;
		int a = ( y1 > y2 ) ? y1 - y2 : y2 - y1; 

		// threshold check and return if within 
		int threshold = (o > a) ? o/10 : a/10;
		if (threshold <= 0) threshold = 5;
		if (Math.abs(o - a) < threshold)  	return;

		if( o > 0 && a > 0 ) { 
			//DPIX if( y2 > y1 ) 	d_pixmap.setPen(0, 255, 255); 
			//DPIX else 		d_pixmap.setPen(255, 0, 255);
			//DPIX d_pixmap.markHLine( x1, x2, y1);
			//DPIX d_pixmap.markHLine( x1, x2, y1+1);
			//DPIX d_pixmap.markHLine( x1, x2, y1-1);
			//DPIX d_pixmap.markVLine( y1, y2, x1);
			//DPIX d_pixmap.markVLine( y1, y2, x1+1);
			//DPIX d_pixmap.markVLine( y1, y2, x1-1);
		}

	}

	void
	d_debugAnchor()
	{
		if(debug) d_markAnchor();
	}

	void
	d_markAnchor()
	{
		if(!pixdebug) return;

		//DPIX d_pixmap.savePen();
		//DPIX d_pixmap.setPen(255, 0, 0);
		int i = 0;
		while( i < mapcount ) {
			int x = xmap[i];
			int y = ymap[i];
			//DPIX d_pixmap.setPixel(x, y - 1);
			//DPIX d_pixmap.setPixel(x, y);
			//DPIX d_pixmap.setPixel(x, y + 1);

			//DPIX d_pixmap.setPixel(x - 1, y - 1);
			//DPIX d_pixmap.setPixel(x - 1, y);
			//DPIX d_pixmap.setPixel(x - 1, y + 1);

			//DPIX d_pixmap.setPixel(x + 1, y - 1);
			//DPIX d_pixmap.setPixel(x + 1, y);
			//DPIX d_pixmap.setPixel(x + 1, y + 1);

			i++;
		}
		//DPIX d_pixmap.restorePen();
	}

	void
	d_markShape()
	{
		if(!pixdebug) return;

		int i = 0;
		while( i < mapcount ) {
			//DPIX d_pixmap.setPixel(xmap[i], ymap[i]);
			i++;
		}
	}

	void
	d_markShape(int linewidth)
	{
		if(!pixdebug) return;

		int i = 0;
		while( i < mapcount  ) {
			//DPIX d_pixmap.setPixel(xmap[i], ymap[i], linewidth);
			i++;
		}
	}

	void
	d_markShapeBounds()
	{
		if(!pixdebug) return;

		d_marklinex( min_x, max_x, min_y);
		d_marklinex( min_x, max_x, max_y);
		d_markliney( min_y, max_y, min_x);
		d_markliney( min_y, max_y, max_x);
		d_marklinex( min_x, max_x, center_y);
		d_markliney( min_y, max_y, center_x);
		//DPIX d_pixmap.markPoint( center_x, center_y, 2);
	}

	void
		d_markShapeLines()
	{
		if(!pixdebug) return;

		int h = max_y - min_y;
		int hd = h / 4;
		if( ! rotated ){ // FIXME
			d_markXLine(min_y + hd);
			d_markXLine(max_y - hd);
		}
	}

	void
	d_markYLine(int x)
	{
		if(!pixdebug) return;

		int min = height;
		int max = 0;	
		int i = 0;
		while( i < mapcount ) {
			if(xmap[i] == x ){
				if( ymap[i] < min ) min = ymap[i];
				if( ymap[i] > max ) max = ymap[i];
			}
			i++;
		}
		d_markliney(min, max, x);

	}

	void
	d_markXLine(int y)
	{
		if(!pixdebug) return;

		int min = width;
		int max = 0;
		int i = 0;
		while( i < mapcount ) {
			if( ymap[i] == y ) { 
				if( xmap[i] < min ) min = xmap[i];
				if( xmap[i] > max ) max = xmap[i];
			}
			i++;
		}
		d_marklinex(min, max, y);
	}

	void
	d_printValues()
	{
		System.out.println( "minx=" + min_x );
		System.out.println( " center_x=" + center_x );
		System.out.println( " maxx=" + max_x );
		System.out.println( " miny=" + min_y );
		System.out.println( " center_y=" + center_y );
		System.out.println( " maxy=" + max_y );

		int i = 0;
		while( i < mapcount ) {
			System.out.println(  xmap[i] + "," + ymap[i]  + " " ); i++;
		}
		System.out.println( );
	}

}
