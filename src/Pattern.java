import java.lang.Math;

class Pattern
{

	static final int TOP_LEFT=1;
	static final int TOP_RIGHT=2;
	static final int BOT_LEFT=3;
	static final int BOT_RIGHT=4;

	static final int SIDE=1;
	static final int BELOW=2;
	static final int ACROSS=3;

	static final int TILT_THRESHOLD=5;

	private Config config;
	private Shape[] shapes;
	private Shape anchor;
	private int nshapes;
	private int code_pivot_x, code_pivot_y; //corner of the code defined by anchor
	private int group_size;      		//growing code group size to identofy the group blocks within
	private int starting_group_size; 	//original code group size 
	private int anchor_at;	     //defaults at top left, but can be at any corner for rotated images
	private int anchor_tilt;     //for tilt correction
	private int anchor_offset;
	private int[] codeblock = new int[12];
	private int[] code = new int[12];
	private int center_x, center_y; 	    //center of the image ( used for rotateShapes )
	private int rotate_delta_x, rotate_delta_y; //grid resize after rotate, delta used in rotateShape()

	//debug only
	private Pixmap pixmap;
	private boolean debug;
	private boolean pixdebug;
	//debug only

	Pattern(Config _config, Shape[] _shapes, int _nshapes, Shape _anchor)
	{
		config   = _config;
		nshapes  = _nshapes;
		shapes   = _shapes;
		anchor   = _anchor;

		center_x = config.GRID_WIDTH/2;
		center_y = config.GRID_HEIGHT/2;

		for(int i = 0; i < 12; i++) codeblock[i] = -9;

		anchor_at    = TOP_LEFT; //default
		anchor_tilt  = 0; 
		code_pivot_x = 0;
		code_pivot_y = 0;
		group_size   = 0;
		starting_group_size  = 0;
		anchor_offset  = 0;
		rotate_delta_x = 0;
		rotate_delta_y = 0;
		debug    = config.DEBUG;
		pixdebug = config.CHECK_VISUAL_DEBUG();
		if(pixdebug) pixmap = config.DBGPIXMAP;
		else         pixmap = null;

	}

	void
	dispose()
	{
	}

	void
	findCode(int[] tag)
	{
		if(pixdebug)      d_writeShapes((String)"selectedshapes");
		if(findTilt())    { 
			rotateShapes();
		}
		if(findTilt())    rotateShapes();//FIXME 
		if(findPattern()) finalPattern(tag);
		if(debug)         d_printPattern();
	}

	boolean
	findPattern()
	{
		int w = (anchor.getWidth() + anchor.getHeight()) / 2 ;
		starting_group_size = w ;
		code_pivot_x = anchor.getminx();
		code_pivot_y = anchor.getminy();
		group_size = starting_group_size;

		locateAnchor(); //works 95% of the time

		if(findBlocks()) return true;
		if(debug) d_printPattern();

		int already_tried_anchor_at = anchor_at;
		for(int i=1; i<5; i++){ //brute force the rest 5% cases
			if( i != already_tried_anchor_at ){
				anchor_at = i;
				group_size = starting_group_size;
				code_pivot_x = anchor.getminx();
				code_pivot_y = anchor.getminy();
				if(findBlocks()) return true;
				if(debug) d_printPattern();
			}
		}
		return false;
	}

	boolean
	findBlocks()
	{
		//DPIX if(pixdebug) pixmap.setPen(255, 0, 0);
		if( ! idGroup( SIDE ))   return false;

		//DPIX if(pixdebug) pixmap.setPen(0, 255, 0);
		if( ! idGroup( BELOW ))  return false;

		//DPIX if(pixdebug) pixmap.setPen(0, 0, 255);
		if( ! idGroup( ACROSS )) return false;


		return true;
	}

	boolean
	idGroup(int gid)
	{
		if(debug) System.out.println( "---------------------------------------------");
		return idGroup( getGroupx(gid), getGroupy(gid), gid, 0);
	}

	boolean
	idGroup(int gid, int delta)
	{
		resizeGroup(delta);
		return idGroup( getGroupx(gid), getGroupy(gid), gid, delta);
	}

	boolean
	idGroup(int x, int y, int gid, int delta)
	{
		int count = 0, i = 0;
		int[] blocks = {-1, -1, -1, -1};
		int within_x = x, within_y = y;
		int within_size_x = group_size, within_size_y = group_size;

		/*DPIX if(pixdebug){
		pixmap.markPoint(x, y, 4);
		pixmap.markVLine(within_y, within_y+within_size_y, within_x);
		pixmap.markVLine(within_y, within_y+within_size_y, within_x+within_size_x);
		pixmap.markHLine(within_x, within_x+within_size_x, within_y);
		pixmap.markHLine(within_x, within_x+within_size_x, within_y+within_size_y);
		}*/

		for(i = 0; i < nshapes; i++){
			if(shapes[i].isWithin(within_x, within_y, within_size_x, within_size_y)){
				blocks[count] = i;
				count++;
				if( count == 4 ) i = nshapes; //break loop
			}
		}
		if( count < 4 ){
			if(debug) System.out.println( "idGroup R ");
			//Keep trying expanding the isWithin() check area 
			if( delta <= starting_group_size && starting_group_size > 0 ) { //limit recursion
				delta = group_size/8;
				return idGroup( gid, delta );	
			}
			if(debug) System.out.println( "Invalid block group" 
				+ ", group id=" + gid + " blocks=" + count );
			return false; //break recursive call
		}else{
			if(debug) System.out.println( "idGroup OK ");
		}
		if(debug) System.out.println("( x=" + x
			+ ", y=" + y 
			+ ", group_size=" + group_size
			+ ", starting_group_size=" + starting_group_size
			+ ", gid=" +  gid 
			+ ", delta=" +  delta 
			+ ") blocks=" + count );

		int b_minx = 0, b_miny = 0;
		int b_maxx = 0, b_maxy = 0;

		count =  blocks[0];
		b_minx = shapes[count].getminx();
		b_miny = shapes[count].getminy();
		b_maxx = shapes[count].getmaxx();
		b_maxy = shapes[count].getmaxy();
		for(i = 1; i < 4; i++){
			count =  blocks[i];
			if(shapes[count].getminx() < b_minx) b_minx = shapes[count].getminx();
			if(shapes[count].getminy() < b_miny) b_miny = shapes[count].getminy();
			if(shapes[count].getmaxx() > b_maxx) b_maxx = shapes[count].getmaxx();
			if(shapes[count].getmaxy() > b_maxy) b_maxy = shapes[count].getmaxy();
		}
		for(i = 0; i < 4; i++){
			count =  blocks[i];
			idBlock(b_minx, b_miny, b_maxx, b_maxy, gid, count);
			/*DPIX if(pixdebug) {
			pixmap.setPen(0,0,0);
			shapes[count].d_markShape(1); 
			pixmap.writeImage( "blocktest");
			pixmap.restorePen();
			}*/
		}
		//DPIX if( pixdebug ) pixmap.writeImage( "group" , gid ); 
		return true;
	}

	void
	idBlock(int minx, int miny, int maxx, int maxy, int gid, int i)
	{
		int bid = locateBlock(minx, miny, maxx, maxy, i);
		int index = ((gid-1) * 4) + bid-1;
		codeblock[index] = matchPattern(i);  	  
		if(debug) System.out.println( "CODE=" + codeblock[index] + "[" + index + " : i=" 
			+ i + " g=" + gid + " b=" + bid + "]" );
	}

	void
	idBlock(int x, int y, int gid, int i, int groupsize_delta)
	{
		int bid = locateBlock(x, y, i, groupsize_delta);
		int index = ((gid-1) * 4) + bid-1;
		codeblock[index] = matchPattern(i); 
		if(debug) System.out.println( "CODE=" + codeblock[index] + "[" + index + " : i=" 
			+ i + " g=" + gid + " b=" + bid + "]" );
	}

	int
	locateBlock(int minx, int miny, int maxx, int maxy, int i)
	{
		int location = 0;
		int gcenter_x = minx + ((maxx-minx)/2);
		int gcenter_y = miny + ((maxy-miny)/2);
		int bcenter_x = shapes[i].getcx();
		int bcenter_y = shapes[i].getcy();
		boolean left = ( bcenter_x <= gcenter_x ) ? true : false;
		boolean top  = ( bcenter_y <= gcenter_y ) ? true : false;
		if(  top &&   left) location =  TOP_LEFT;
		if(  top && ! left) location =  TOP_RIGHT;
		if(! top &&   left) location =  BOT_LEFT;
		if(! top && ! left) location =  BOT_RIGHT;
		return location;
	}

	int
	locateBlock(int x, int y, int i, int groupsize_delta)
	{
		int within_size = group_size ;
		int location = 0;
		int gcx = x + within_size / 2;
		int gcy = y + within_size / 2;
		int bcx = shapes[i].getcx();
		int bcy = shapes[i].getcy();
		boolean left = ( bcx <= gcx ) ? true : false;
		boolean top  = ( bcy <= gcy ) ? true : false;
		if(  top &&   left) location =  TOP_LEFT;
		if(  top && ! left) location =  TOP_RIGHT;
		if(! top &&   left) location =  BOT_LEFT;
		if(! top && ! left) location =  BOT_RIGHT;
		return location;
	}

	int
	matchPattern(int i)
	{
		return shapes[i].matchPattern();
	}

	void
	resizeGroup(int delta)
	{
		code_pivot_x -= delta;
		code_pivot_y -= delta;
		group_size   += (2*delta);
	}

	//TODO- Use a transform matrix for this lookup
	int
	getGroupx(int id)
	{
		int x = code_pivot_x; 
		switch (anchor_at){
			case TOP_LEFT:
				switch (id){
			case SIDE:
				x = code_pivot_x + group_size;	
				break;
			case BELOW:
				x = code_pivot_x;
				break;
			case ACROSS:
				x = code_pivot_x + group_size;	
				break;
				}
				break;
			case TOP_RIGHT:
				switch (id){
			case SIDE:
				x = code_pivot_x - anchor_offset ;	
				break;
			case BELOW:
				x = code_pivot_x - group_size - anchor_offset;
				break;
			case ACROSS:
				x = code_pivot_x - group_size - anchor_offset;
				break;
				}
				break;
			case BOT_LEFT:
				switch (id){
			case SIDE:
				x = code_pivot_x;	
				break;
			case BELOW:
				x = code_pivot_x + group_size;
				break;
			case ACROSS:
				x = code_pivot_x + group_size;	
				break;
				}
				break;
			case BOT_RIGHT:
				switch (id){
			case SIDE:
				x = code_pivot_x - group_size - anchor_offset;
				break;
			case BELOW:
				x = code_pivot_x - anchor_offset ;
				break;
			case ACROSS:
				x = code_pivot_x - group_size - anchor_offset;
				break;
				}
				break;
			default:
				break;
		}
		return x;
	}

	//TODO- Use a transform matrix for this lookup
	int
	getGroupy(int id)
	{
		int y = code_pivot_y; 
		switch (anchor_at){
			case TOP_LEFT:
				switch (id){
			case SIDE:
				y = code_pivot_y;
				break;
			case BELOW:
				y = code_pivot_y + group_size;	
				break;
			case ACROSS:
				y = code_pivot_y + group_size;	
				break;
				}
				break;
			case TOP_RIGHT:
				switch (id){
			case SIDE:
				y = code_pivot_y + group_size;	
				break;
			case BELOW:
				y = code_pivot_y ;
				break;
			case ACROSS:
				y = code_pivot_y + group_size;	
				break;
				}
				break;
			case BOT_LEFT:
				switch (id){
			case SIDE:
				y = code_pivot_y - group_size - anchor_offset;
				break;
			case BELOW:
				y = code_pivot_y - anchor_offset;
				break;
			case ACROSS:
				y = code_pivot_y - group_size - anchor_offset;
				break;
				}
				break;
			case BOT_RIGHT:
				switch (id){
			case SIDE:
				y = code_pivot_y - anchor_offset;	
				break;
			case BELOW:
				y = code_pivot_y - group_size - anchor_offset;
				break;
			case ACROSS:
				y = code_pivot_y - group_size - anchor_offset;
				break;
				}
				break;
			default:
				break;
		}
		return y;
	}


	void
	locateAnchor()
	{

		int a_center_x = anchor.getcx();
		int a_center_y = anchor.getcy();
		int g_center_x = anchor.getgridw() / 2;
		int g_center_y = anchor.getgridh() / 2;
		boolean left = ( a_center_x <= g_center_x ) ? true : false;
		boolean top  = ( a_center_y <= g_center_y ) ? true : false;
		if(  top &&   left) anchor_at =  TOP_LEFT;
		if(! top &&   left) anchor_at =  BOT_LEFT;
		if(  top && ! left) anchor_at =  TOP_RIGHT;
		if(! top && ! left) anchor_at =  BOT_RIGHT;

		/*KEEP 
		int tl_c = 0, bl_c = 0, tr_c = 0, br_c = 0;
		int gw = config.GRID_WIDTH;
		int gh = config.GRID_HEIGHT;
		int w = anchor.getWidth()*2;
		int h = anchor.getHeight()*2;

		int x = anchor.getminx();
		int y = anchor.getminy();
		if( x > 0 && x < gw && y > 0 && y < gh ){
		for(int i = 0; i < nshapes; i++)  if(shapes[i].isWithin(x, y, w, h)) tl_c++;
		}

		x = anchor.getmaxx()-w;
		y = anchor.getminy();
		if( x > 0 && x < gw && y > 0 && y < gh ){
		for(int i = 0; i < nshapes; i++) if(shapes[i].isWithin(x, y, w, h)) tr_c++;
		}

		x = anchor.getminx();
		y = anchor.getmaxy()-h;
		if( x > 0 && x < gw && y > 0 && y < gh ){
		for(int i = 0; i < nshapes; i++) if(shapes[i].isWithin(x, y, w, h)) bl_c++;
		}

		x = anchor.getmaxx()-w;
		y = anchor.getmaxy()-h;
		if( x > 0 && x < gw && y > 0 && y < gh ){
		for(int i = 0; i < nshapes; i++) if(shapes[i].isWithin(x, y, w, h)) br_c++;
		}

		PORTME System.out.println( tl_c + " " + tr_c + " " + br_c + " " + bl_c );
		*/
	}

	boolean
	findTilt()
	{
		anchor_tilt = anchor.findTilt();
		return  Math.abs(anchor_tilt) > 2  ? true : false;
	}

	/*CLDC10
	int
	findAngle(int x1, int y1, int x2, int y2, int orientation)
	{
		int angle;	
		int o = IntMath.abs(x1 - x2);
		int a = IntMath.abs(y2 - y1); 
		if( o <= 0 || a <= 0 ) return 0;
		angle = IntMath.tan(o/a)* 180000 / 3141;
		if( angle > 45 ) angle = 90 - angle;
		if (debug) {
			System.out.println( "Tilt: x1=" + x1 + ", y1=" + y1 );
			System.out.println( " x2=" + x2 + ", y2=" + y2 );
			System.out.println( " o=" + o  + ", a=" + a );
			System.out.println( " Angle=" + angle );
			System.out.println( " at=" + anchor_at );
		}
		return (int)angle;
	}
	CLDC10*/
	//_CLDC11
	int
    findAngle(int x1, int y1, int x2, int y2, int orientation)
    {
        double angle;
        double o = Math.abs(x1 - x2);
        double a = Math.abs(y2 - y1);
        if( o <= 0 || a <= 0 ) return 0;
        angle = Math.tan(o/a)* 180 / 3.14159265;
        if( angle > 45 ) angle = 90 - angle;
        if (debug) {
            System.out.println( "Tilt: x1=" + x1 + ", y1=" + y1 );
            System.out.println( " x2=" + x2 + ", y2=" + y2 );
            System.out.println( " o=" + o  + ", a=" + a );
            System.out.println( " Angle=" + angle );
            System.out.println( " at=" + anchor_at );
        }
        return (int)angle;
    }
	//CLDC11_

	/*CLDC10
	void
	computeRotatedGrid(int angle)
	{
		int d = angle * -1;
		int min_x=0, min_y=0, max_x=config.GRID_WIDTH, max_y=config.GRID_HEIGHT;

		int a =  ( (3141 * d) / 180000 );
		int a1 = IntMath.cos(a);
		int a2 = IntMath.sin(a);
		int rx1 = (int)((min_x - center_x) * a1 + (min_y - center_y) * a2 + center_x);
		int ry1 = (int)((-(min_x - center_x)) * a2 + (min_y - center_y) * a1 + center_y);
		int rx2 = (int)((max_x - center_x) * a1 + (min_y - center_y) * a2 + center_x);
		int ry2 = (int)((-(max_x - center_x)) * a2 + (min_y - center_y) * a1 + center_y);
		int rx3 = (int)((min_x - center_x) * a1 + (max_y - center_y) * a2 + center_x);
		int ry3 = (int)((-(min_x - center_x)) * a2 + (max_y - center_y) * a1 + center_y);
		int rx4 = (int)((max_x - center_x) * a1 + (max_y - center_y) * a2 + center_x);
		int ry4 = (int)((-(max_x - center_x)) * a2 + (max_y - center_y) * a1 + center_y);
		int rminx = rx1 < rx2 ? rx1 : rx2;
		if(rx3 < rminx) rminx = rx3;
		if(rx4 < rminx) rminx = rx4;
		int rminy = ry1 < ry2 ? ry1 : ry2;
		if(ry3 < rminy) rminy = ry3;
		if(ry4 < rminy) rminy = ry4;
		int rmaxx = rx1 > rx2 ? rx1 : rx2;
		if(rx3 > rmaxx) rmaxx = rx3;
		if(rx4 > rmaxx) rmaxx = rx4;
		int rmaxy = ry1 > ry2 ? ry1 : ry2;
		if(ry3 > rmaxy) rmaxy = ry3;
		if(ry4 > rmaxy) rmaxy = ry4;
		if(rminx > 0 || rminy > 0){
			System.out.println( "ASSERT: min values should be <= 0, rminx=" + rminx + " rminy=" + rminy );
			if(debug) System.exit(1);
		}
		rotate_delta_x = Math.abs(rminx);
		rotate_delta_y = Math.abs(rminy);
		config.GRID_HEIGHT = rmaxy - rminy;
		config.GRID_WIDTH  = rmaxx - rminx;
		center_x+=rotate_delta_x;
		center_y+=rotate_delta_y;
		//PORTME if(pixdebug) pixmap.resizePixmap(config.GRID_WIDTH, config.GRID_HEIGHT);
		if(debug ) System.out.println( "[" + rminx + " " + rminy + " " + rmaxx + " " + rmaxy 
			+ " d" + rotate_delta_x + " " + rotate_delta_y + " g" + config.GRID_WIDTH 
			+ " " + config.GRID_HEIGHT + "]" );
	}
	CLDC10*/
	//_CLDC11
	void
    computeRotatedGrid(int angle)
    {
        int d = angle * -1;
        int min_x=0, min_y=0, max_x=config.GRID_WIDTH, max_y=config.GRID_HEIGHT;

        double a = (double) ( (3.1415926535897931 * (double)d) / 180 );
        double a1 = Math.cos(a);
        double a2 = Math.sin(a);
        int rx1 = (int)((double)(min_x - center_x) * a1 + (double)(min_y - center_y) * a2 + (double)center_x);
        int ry1 = (int)((double)(-(min_x - center_x)) * a2 + (double)(min_y - center_y) * a1 + (double)center_y);
        int rx2 = (int)((double)(max_x - center_x) * a1 + (double)(min_y - center_y) * a2 + (double)center_x);
        int ry2 = (int)((double)(-(max_x - center_x)) * a2 + (double)(min_y - center_y) * a1 + (double)center_y);
        int rx3 = (int)((double)(min_x - center_x) * a1 + (double)(max_y - center_y) * a2 + (double)center_x);
        int ry3 = (int)((double)(-(min_x - center_x)) * a2 + (double)(max_y - center_y) * a1 + (double)center_y);
        int rx4 = (int)((double)(max_x - center_x) * a1 + (double)(max_y - center_y) * a2 + (double)center_x);
        int ry4 = (int)((double)(-(max_x - center_x)) * a2 + (double)(max_y - center_y) * a1 + (double)center_y);
        int rminx = rx1 < rx2 ? rx1 : rx2;
        if(rx3 < rminx) rminx = rx3;
        if(rx4 < rminx) rminx = rx4;
        int rminy = ry1 < ry2 ? ry1 : ry2;
        if(ry3 < rminy) rminy = ry3;
        if(ry4 < rminy) rminy = ry4;
        int rmaxx = rx1 > rx2 ? rx1 : rx2;
        if(rx3 > rmaxx) rmaxx = rx3;
        if(rx4 > rmaxx) rmaxx = rx4;
        int rmaxy = ry1 > ry2 ? ry1 : ry2;
        if(ry3 > rmaxy) rmaxy = ry3;
        if(ry4 > rmaxy) rmaxy = ry4;
        if(rminx > 0 || rminy > 0){
            System.out.println( "ASSERT: min values should be <= 0, rminx=" + rminx + " rminy=" + rminy );
            if(debug) System.exit(1);
        }
        rotate_delta_x = Math.abs(rminx);
        rotate_delta_y = Math.abs(rminy);
        config.GRID_HEIGHT = rmaxy - rminy;
        config.GRID_WIDTH  = rmaxx - rminx;
        center_x+=rotate_delta_x;
        center_y+=rotate_delta_y;
        //PORTME if(pixdebug) pixmap.resizePixmap(config.GRID_WIDTH, config.GRID_HEIGHT);
        if(debug ) System.out.println( "[" + rminx + " " + rminy + " " + rmaxx + " " + rmaxy
            + " d" + rotate_delta_x + " " + rotate_delta_y + " g" + config.GRID_WIDTH
            + " " + config.GRID_HEIGHT + "]" );
    }
	//CLDC11_

	void
	rotateShapes()
	{
		if( anchor_tilt == 0 ) return;
		if(config.PESSIMISTIC_ROTATION) computeRotatedGrid(anchor_tilt);

		//DPIX if(pixdebug) pixmap.clearPixmap();

		for(int i = 0; i < nshapes; i++){
			shapes[i].rotateShape(center_x, center_y, rotate_delta_x, rotate_delta_y, anchor_tilt);
			if(pixdebug) shapes[i].d_markShape(); 
			//DPIX if(debug && pixdebug) pixmap.writeImage( "rotate2", i );
		}
		anchor.rotateShape(center_x, center_y, rotate_delta_x, rotate_delta_y, anchor_tilt);

		if(pixdebug) anchor.d_markShape();
		if(pixdebug) d_writeShapes((String)"rotatedshapes");
	}

	void
	printCodeBlock()
	{
		for(int i = 0; i < 12; i++) System.out.print(codeblock[i] +  " ");
		System.out.println("");	
	}

	boolean
	validPattern()
	{
		for(int i = 0; i < 12; i++) { 
			if (codeblock[i] < 0) return false;
		}
		return true;
	}

	void
	finalPattern(int[] tag)
	{
		Matrix matrix = new Matrix();
		if( config.RETURN_INTERNAL_CODE ) {
			for(int i = 0; i < 12; i++) tag[i] = matrix.rotateBlock(anchor_at, codeblock[matrix.transCode(anchor_at, i)]);
		} else {
			for(int i = 0; i < 12; i++) tag[i] = matrix.rotateBlock(anchor_at, codeblock[i]);
		}
		matrix = null;
	}


	void
	d_printPattern()
	{
		Matrix matrix = new Matrix();
		if( debug ){
			System.out.println( "PATTERN   : ");
			for(int i = 0; i < 12; i++) { 
				System.out.print( codeblock[i] + " ");
				if( (i+1) % 4  == 0 ) System.out.println( " ");
			}
			System.out.println();

			System.out.println( "PATTERN TC: ");
			for(int i = 0; i < 12; i++) { 
				System.out.print( codeblock[matrix.transCode(anchor_at, i)] + " ");
				if( (i+1) % 4  == 0 ) System.out.print( " ");
			}
			System.out.println();

			System.out.println( "PATTERN RB: ");
			for(int i = 0; i < 12; i++) { 
				System.out.print( matrix.rotateBlock(anchor_at, codeblock[i])  + " ");
				if( (i+1) % 4  == 0 ) System.out.print( " ");
			}
			System.out.println();

			System.out.println( "PATTERN TR: ");
			for(int i = 0; i < 12; i++) { 
				System.out.print( matrix.rotateBlock(anchor_at, codeblock[matrix.transCode(anchor_at, i)])  + " ");
				if( (i+1) % 4  == 0 ) System.out.print( " ");
			}
			System.out.println();
		}
		matrix = null;
	}

	void
	d_debugShapes()
	{
		if(pixdebug) d_writeShapes();
	}

	void
	d_writeShapes(String filename)
	{
		d_writeShapes();
		//DPIX pixmap.writeImage(filename);
	}

	void
	d_writeShapes()
	{
		for (int i = 0; i < nshapes; i++) shapes[i].d_markShape();
		anchor.d_markAnchor();
	}
}
