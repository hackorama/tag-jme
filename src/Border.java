import java.util.Vector;
import java.lang.Exception;

class Border
{
	static final int BLACK=0;
	static final int GREEN=1;
	static final int RED=2;
	static final int BLUE=3;
	static final int YELLOW=4;
	static final int AQUA=5;
	static final int PINK=6;
	static final int WHITE=7;

	private Config config;
	private Shape[] shapes;
	private Shape anchor;  // selected anchor from current 
	private Shape[] anchors;
	private Shape current; // the current shape being processed
	private Pixmap pixmap;
	private Pattern pattern;
	private boolean[] edgemap;
	private int  width, height;
	private int  min_threshold, max_threshold;
	private int  shapes_found;
	private int  anchors_found;
	private int[] widths_holder;
	private int[] heights_holder;
	private int[] w_midpoints_holder;
	private int[] h_midpoints_holder;
	private int mapcount;
	private int max_shapes;
	private int max_anchors;

	//globals for recursion
	private Vector xmap; //dynamic holder for shape x values
	private Vector ymap; //dynamic holder for shape y values
	private int min_x, min_y, max_x, max_y;
	private int tx, ty;
	private int startx, starty, seg_count;
	//globals for recursion

	//debug only
	private boolean debug;
	private boolean pixdebug;
	private boolean anchordebug;
	int BORDERCOLOR; //FIXME: remove after debug
	//debug only

	Border(Config _config, Shape[] _shapes, Shape _anchor)
	{
		config = _config;
		shapes = _shapes;
		anchor = _anchor;

		debug       = config.DEBUG;
		pixdebug    = config.CHECK_VISUAL_DEBUG();
		anchordebug = config.ANCHOR_DEBUG;
		max_anchors = config.MAX_ANCHORS;
		max_shapes  = config.MAX_SHAPES;

		for(int i = 0; i < max_shapes; i++) shapes[i].setConfig(config);

		if(pixdebug) pixmap = config.DBGPIXMAP;
		else         pixmap = null;

		edgemap = config.EDGE_MAP;
		if(edgemap == null) {
			System.out.println("ASSERT: Empty edgemap");
			if(debug) System.exit(1);
		}
		width = config.GRID_WIDTH;
		height = config.GRID_HEIGHT;
		min_threshold = ( width > height ) ? width/20: height/20;
		max_threshold = ( width < height ) ? width/2 : height/2; //changed from 4 to account rotated ones

		current = new Shape(config);
		anchors = new Shape[max_anchors];
		for(int i=0; i < max_anchors; i++) anchors[i] = new Shape();//JONLY
		for(int i=0; i < max_anchors; i++) anchors[i].setConfig(config);

		shapes_found  = 0;
		anchors_found = 0;
		widths_holder = new int[height];
		heights_holder = new int[width];
		for(int i=0; i<height; i++) widths_holder[i] = 0;
		for(int i=0; i<width; i++)  heights_holder[i] = 0;
		w_midpoints_holder = new int[height];
		h_midpoints_holder = new int[width];
		for(int i=0; i<height; i++) w_midpoints_holder[i] = 0;
		for(int i=0; i<width; i++)  h_midpoints_holder[i] = 0;

		if(pixdebug) { 
			pixmap.resizePixmap(config.GRID_WIDTH, config.GRID_HEIGHT);
			pixmap.clearPixmap(); //reset after scaling
			current.d_setPixmap(pixmap);
			anchor.d_setPixmap(pixmap);
		} 
		xmap = new Vector();
		ymap = new Vector();

	}

	void
	dispose()
	{
		config.freeEdgemap();
		current = null;
		anchors = null;
		widths_holder = null;
		heights_holder = null;
		w_midpoints_holder = null;
		h_midpoints_holder = null;
	}

	Shape[]
	getShapes()
	{
		return shapes;
	}

	Shape
	getAnchor()
	{
		return anchor;
	}

	int
	getShapeCount()
	{
		return shapes_found;
	}

	int
	findShapes()
	{
		getBorders();
		if( foundShapes() ) { 
			if(! foundAnchor()) findAnchor(); 
			if(foundAnchor()) return shapes_found;
		}
		return 0;
	}

	/* Sigle pass for recursive edge tracing 
	* and shape length based selection filterShape()
	* TODO: parsing stsrts from top-left corner
	*       evaluate parsing from center point
	*       could bail out bad tags early 
	*       and avoid noise on edge shapes 
	* TODO: bail out bad tags ideas 
	* 	1. shape density 
	*       2. border points density 
	*/
	void
	getBorders()
	{
		/*PORTME #ifdefPERF_DEBUG
		clock_t start =  clock() * CLK_TCK;
		#endif PORTME */
		int i=0, j=0, count = 0;
		if(pixdebug) pixmap.debugImage( "border", count++ );
		for(j=0; j<height; j++){
			for(i=0; i<width; i++){
				if( isEdge(i, j) ){
					BORDERCOLOR = (count++%4)+3; //FIXME: Remove after debug

					// reset globals
					min_x = i; min_y = j; max_x = i; max_y = j;
					tx = 0; ty = 0;
					startx = i; starty = j;
					xmap.removeAllElements();
					ymap.removeAllElements();
					seg_count = 0;
					resetWidthsAndHeights();

					// trace
					borderTrace(i, j);
					markBorder(i, j); 

					// verify and keep stored values
					if(filterShape()) {  
						addShape();
					}else if(pixdebug) { 
						pixmap.setPen( pixmap.maxRGB(), pixmap.maxRGB(), pixmap.maxRGB() );
						pixmap.debugImage("skipped");
					}
				}
			}
		}
		/*PORTME #ifdefPERF_DEBUG
		clock_t end =  clock() * CLK_TCK;
		System.out.println( "Border Trace : " + ((float)end-start)/(float)1000000 + "secs" );
		#endif PORTME */
		if( pixdebug ){
			pixmap.writeImage("allshapes");
			pixmap.clearPixmap();
			pixmap.setPen(0, 0, 0);
		}
	}

	void
	resetWidthsAndHeights()
	{
		widths_holder = new int[height];
		heights_holder = new int[width];
		w_midpoints_holder = new int[height];
		h_midpoints_holder = new int[width];
		for(int i=0; i<height; i++) widths_holder[i] = 0;
		for(int i=0; i<height; i++) w_midpoints_holder[i] = 0;
		for(int i=0; i<width; i++)  heights_holder[i] = 0;
		for(int i=0; i<width; i++)  h_midpoints_holder[i] = 0;
	}

	void
	addWidths(int x, int y)
	{
		if(widths_holder[y] == 0){
			widths_holder[y]      = x*-1;
			w_midpoints_holder[y] = x;
		}else if(widths_holder[y] < 0){
			int diff = Math.abs(widths_holder[y]+x);
			if(diff > 8)  {
				//w_midpoints_holder[y] = Math.abs(widths_holder[y])+(diff/2);
				int lastx = Math.abs(widths_holder[y]);
				w_midpoints_holder[y] = x > lastx ? lastx+(diff/2) : x+(diff/2);
				widths_holder[y]      = diff; 
			}
		}
	}

	void
	addHeights(int x, int y)
	{
		if(heights_holder[x] == 0){
			heights_holder[x]     = y*-1;
			h_midpoints_holder[x] = y;
		}else if(heights_holder[x] < 0){
			int diff = Math.abs(heights_holder[x]+y);
			if(diff > 8) {
				//h_midpoints_holder[x] = Math.abs(heights_holder[x])+(diff/2);
				int lasty = Math.abs(heights_holder[x]);
				h_midpoints_holder[x] = y > lasty ? lasty+(diff/2) : y+(diff/2);
				heights_holder[x]     = diff;
			}
		}
	}

	boolean
	borderTrace(int x, int y)
	{
		if (x < min_x) min_x = x;
		if (x > max_x) max_x = x;
		if (y < min_y) min_y = y;
		if (y > max_y) max_y = y;
		if( seg_count > 0){
			if (x == startx && y == starty) return true; // closed border 
			else                            markBorder( x, y ); 
		}
		xmap.addElement(new Integer(x));
		ymap.addElement(new Integer(y));
		addWidths(x,y);
		addHeights(x,y);
		tx+=x;
		ty+=y;
		seg_count++;

		int i = 1;
		if(isEdge(  x, y+i)) borderTrace(  x, y+i);
		if(isEdge(  x, y-i)) borderTrace(  x, y-i); 

		if(isEdge(x+i, y-i)) borderTrace(x+i, y-i);
		if(isEdge(x+i, y  )) borderTrace(x+i, y  );
		if(isEdge(x+i, y+i)) borderTrace(x+i, y+i);

		if(isEdge(x-i, y+i)) borderTrace(x-i, y+i);
		if(isEdge(x-i, y  )) borderTrace(x-i, y);
		if(isEdge(x-i, y-i)) borderTrace(x-i, y-i);
		return false; // open border
	}

	boolean
	foundShapes()
	{
		if( shapes_found < 12 )        return false;
		return true;
	}

	boolean
	foundAnchor()
	{
		if( anchor.size() <= 0 ) return false;
		return true;
	}

	/* 
	* Check if current shape matches anchor features
	* Replace selected anchor if this one is bigger 
	* TODO: size checked by bounding box area Shape::size()
	*       should we use the Shape::length() instead 
	*/
	void
	anchorCheck()
	{
		current.setValues(xmap, ymap, seg_count);
		current.setBounds(min_x, min_y, max_x, max_y);
		current.setCenter( min_x + (max_x - min_x)/2, min_y + (max_y - min_y)/2 );
		current.setWidthValues(widths_holder, w_midpoints_holder, min_y, max_y, false);
		current.setHeightValues(heights_holder, h_midpoints_holder, min_x, max_x, false);
		if(anchordebug) System.out.println( "[ anchorCheck " );

		if( current.isAnchor() ){ //level=0 strictest select as final anchor
			if(anchordebug) System.out.println( current.size() + "-" + anchor.size() );
			if( current.size() > anchor.size() ){ //larger block over rides last found anchor 
				anchor.setValues(xmap, ymap, seg_count);
				anchor.setBounds(min_x, min_y, max_x, max_y);
				anchor.setCenter( min_x + (max_x - min_x)/2, min_y + (max_y - min_y)/2 );
				anchor.copyWidthValues(current.getWidthValues(), current.getWMidpointValues(),  min_y, max_y);
				anchor.copyHeightValues(current.getHeightValues(), current.getHMidpointValues(), min_x, max_x);
				if(anchordebug) System.out.println( " Found : " + anchor.size());
			}
		}else if( current.isAnchorLike() ){ //level=3 loosest, add to possible list of anchors
			addAnchor();
		}
		if(anchordebug) System.out.println( "anchorCheck ]"  );
	}

	/*
	* if the first threshold based pass did not find an anchor
	* this could be a tilted or abnormal sized one 
	* this pass selects the biggest block from the 
	* threshold selection as an anchor if it passes the square check
	* 
	* Instaed of Shape::isSquare() uses a more loose check Shape::isAnchorLevel2()
	*
	* NOTE: Returns true only on finding a new one 
	* Return False, If there is an existing one or we could not find a new one
	*
	*/
	//findAnchor(int level)
	boolean
	findAnchor()
	{
		int maxsize = 0;
		int shapeindex = -1, anchorindex = -1;
		if( anchor.size() > 0 ) return false; //already have an anchor
		if(anchordebug) System.out.println( "[findAnchor " );
		for (int i = 0; i < shapes_found; i++){
			if( shapes[i].size() > maxsize ) { 
				if( shapes[i].isAnchor() ) {
					/*if(anchordebug) System.out.println( "shapes[" + i + "] size=" 
					+ shapes[i].size() + " max=" + maxsize );*/
					maxsize = shapes[i].size();
					shapeindex = i;
				}
			}
		}
		for (int i = 0; i < anchors_found; i++){
			if( anchors[i].size() > maxsize ) { 
				if( anchors[i].isAnchor() ) {
					/*if(anchordebug) System.out.println( "anchors[" + i + "] size=" 
					+ anchors[i].size() + "max=" + maxsize );*/
					maxsize = anchors[i].size();
					anchorindex = i;
				}
			}
		}
		if(anchordebug)          System.out.println( "findAnchor ]" );
		if(anchorindex >= 0)     copyAnchor(anchors[anchorindex]);
		else if(shapeindex >= 0) copyAnchor(shapes[shapeindex]);
		else   			 return false;
		if(pixdebug){
			anchor.d_debugAnchor();
			anchor.d_markAnchor();
			pixmap.writeImage( "anchor-level" ); 
		}
		if(anchordebug) { 
			System.out.println( "Found Anchor " );
			if(anchorindex >= 0)  System.out.println( " index=" + anchorindex + " in anchors" );
			else  		      System.out.println( " index=" + shapeindex + " in shapes" );
		}
		return true; //found a new one, with relaxed check
	}

	void
	copyAnchor(Shape shape)
	{
		int mnx =0, mny = 0, mxx = 0, mxy = 0;
		mnx = shape.getminx();
		mny = shape.getminy();
		mxx = shape.getmaxx();
		mxy = shape.getmaxy();
		anchor.copyValues(shape.getxmap(), shape.getymap(), shape.getmapcount());
		anchor.setBounds(mnx, mny, mxx, mxy);
		anchor.setCenter( mnx + (mxx - mnx)/2, mny + (mxy - mny)/2 );
		//anchor.setGrid( pixmap.getWidth(),  pixmap.getHeight() );
		anchor.copyWidthValues(shape.getWidthValues(), shape.getWMidpointValues(), mny, mxy);
		anchor.copyHeightValues(shape.getHeightValues(), shape.getHMidpointValues(), mnx, mxx);
	}

	void
	addAnchor()
	{
		if( anchors_found >= max_anchors ){
			if(debug) System.out.println( "ERROR: MAX ANCHOR SHAPES " );
			return;
		}
		anchors[anchors_found].setValues(xmap, ymap, seg_count);
		anchors[anchors_found].setWidthValues(widths_holder, w_midpoints_holder, min_y, max_y, true);
		anchors[anchors_found].setHeightValues(heights_holder, h_midpoints_holder, min_x, max_x, true);
		anchors[anchors_found].setBounds(min_x, min_y, max_x, max_y);
		anchors[anchors_found].setCenter(min_x + (max_x - min_x)/2, min_y + (max_y - min_y)/2);
		if(anchordebug) System.out.println( "addAnchor :" + anchors[anchors_found].size() + " (" + anchors_found + ")" );
		/*
		if(debug){
		anchors[anchors_found].d_printWidths();
		anchors[anchors_found].d_printHeights();
		}*/
		anchors_found++;
	}

	void
	addShape()
	{
		if( shapes_found >= max_shapes ){
			if(debug) System.out.println( "ERROR: MAX SHAPES " );
			return;
		}
		shapes[shapes_found].setValues(xmap, ymap, seg_count);
		shapes[shapes_found].setWidthValues(widths_holder, w_midpoints_holder, min_y, max_y, true);
		shapes[shapes_found].setHeightValues(heights_holder, h_midpoints_holder, min_x, max_x, true);
		shapes[shapes_found].setBounds(min_x, min_y, max_x, max_y);
		shapes[shapes_found].setCenter( min_x + (max_x - min_x)/2, min_y + (max_y - min_y)/2 );
		shapes[shapes_found].d_setPixmap(pixmap);
		/*if(debug){
		shapes[shapes_found].d_printWidths();
		shapes[shapes_found].d_printHeights();
		}*/

		shapes_found++;
		if(pixdebug){
			pixmap.debugImage("shape-" + (shapes_found-1) ); 
		}
	}

	void
	filterAnchor()
	{
		if(!foundAnchor()) anchorCheck();
	}

	/*
	* bounding box size based threshold 
	* TODO : Add total length Shape:Llength() based threshold 
	*        to weed out boudning box fitting non blocks
	*
	*/
	boolean
	filterShape()
	{
		int length = xmap.size();
		if(debug){ 
			System.out.println("");
			System.out.print( length + " "  + min_x + ", " + min_y  + "  " + max_x + ", " + max_y );
			System.out.print( " [" + min_threshold + "-" +  max_threshold + "] " );
			System.out.print(  max_x - min_x + " " );
			System.out.print(  max_y - min_y + " " );
		}
		if(((max_x - min_x) < min_threshold || (max_y - min_y) < min_threshold)
			&& length < (2*min_threshold) ) { //extra check for rotated 6 and 7 shapes 
				if(debug) System.out.print( " \t: - "  );
		} else if( (max_x - min_x) > max_threshold ||  (max_y - min_y) > max_threshold ) {
			if(debug) System.out.print( " \t: + "  );
			filterAnchor();
		}else{
			if(debug) System.out.print( " \t: *  " );
			return true;
		}
		return false;
	}

	void
	markBorder(int x, int y)
	{
		edgemap[(y*width)+x] = false;
		if(pixdebug) d_setColor(x, y, BORDERCOLOR);
	}

	boolean
	isEdge(int x, int y)
	{
		return edgemap[(y*width)+x];
	}

	void
	d_setColor(int x, int y, int color)
	{
		if( color == RED )    d_setColor256RGB(x, y, 255, 0, 0);
		if( color == GREEN )  d_setColor256RGB(x, y, 0, 255, 0);
		if( color == BLUE )   d_setColor256RGB(x, y, 0, 0, 255);
		if( color == YELLOW ) d_setColor256RGB(x, y, 255, 215, 0);
		if( color == PINK )   d_setColor256RGB(x, y, 255, 0, 255);
		if( color == AQUA )   d_setColor256RGB(x, y, 0, 128, 255);
	}

	void
	d_setColor256RGB(int x, int y, int r, int g, int b)
	{
		if(pixdebug) pixmap.setPixel256RGB(x, y, r, g, b);
	}

	void
	d_setColor(int x, int y, int r, int g, int b)
	{
		if(pixdebug) pixmap.setPixel(x, y, r, g, b);
	}

	void
	d_debugShapes(String filename)
	{
		if(pixdebug) d_writeShapes(filename);
	}

	void
	d_debugShapes()
	{
		if(pixdebug) d_writeShapes();
	}

	void
	d_writeShapes(String filename)
	{
		if(!pixdebug) return;

		d_writeShapes();
		pixmap.writeImage(filename);
	}

	void
	d_writeShapes()
	{
		if(!pixdebug) return;

		pixmap.clearPixmap();
		for (int i = 0; i < shapes_found; i++) shapes[i].d_markShape(); 
		if( debug ) { 
			System.out.print( "Code Blocks = " + shapes_found );
			if(anchor.size() <= 0)  System.out.println( ", no Anchor");  
			else			System.out.println("");
		}
	}

}
