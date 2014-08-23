import java.lang.String;

public class Config{

	/*
	static const bool PLATFORM_CPP = false;
	static const bool PLATFORM_CPP_MAGICK = false;
	static const bool PLATFORM_CPP_SYMBIAN = false;
	static const bool PLATFORM_CPP_SYMBIAN_S60 = false;
	*/

	static final boolean PLATFORM_JAVA 				= false;
	static final boolean PLATFORM_JAVA_ME 			= false;
	static final boolean PLATFORM_JAVA_ME_CLDC1_0 	= true;
	static final boolean PLATFORM_JAVA_ME_CLDC1_1 	= false;
	static final boolean PLATFORM_JAVA_SE 		= false;
	static final boolean PLATFORM_JAVA_ANDROID 	= false;

	static final int MAX_ANCHORS=12;
	static final int MAX_SHAPES=48;

	boolean RETURN_INTERNAL_CODE = false;

	int THRESHOLD_WINDOW_SIZE = 48;
	int THRESHOLD_OFFSET = 20;
	int THRESHOLD_RGB_FACTOR = 1; //IMAGEMAGIC=256 JSE=1 JME=1

	int PIXMAP_SCALE_SIZE = 320;  //must be > THRESHOLD_WINDOW_SIZE
	boolean PIXMAP_NATIVE_SCALE = false;
	boolean PIXMAP_FAST_SCALE = true;  //not effective when PIXMAP_NATIVE_SCALE == true

	int ANCHOR_BOX_FLEX_PERCENT = 30;
	int SHAPE_BOX_FLEX_PERCENT = 30;

	int GRID_WIDTH = 0;
	int GRID_HEIGHT = 0;

	boolean PESSIMISTIC_ROTATION = true;

	Pixmap DBGPIXMAP = null;

	boolean EDGE_MAP[] = null;

	String TAG_IMAGE_FILE = "";

	boolean DEBUG         = false;
	boolean VISUAL_DEBUG  = false;
	boolean ANCHOR_DEBUG  = false;

	boolean ARGS_OK = false;

	public void 
	Config()
	{

	}

	public void 
	dispose()
	{
		DBGPIXMAP = null;
		EDGE_MAP = null;
		TAG_IMAGE_FILE = null;
	}

	void 
	freeEdgemap() //free from border 
	{
		EDGE_MAP = null;
	}

	boolean 
	CHECK_VISUAL_DEBUG()
	{
		if(DBGPIXMAP == null) return false;
		return VISUAL_DEBUG;
	}

	void 
	setDebugPixmap(Pixmap _pixmap)
	{
		DBGPIXMAP = _pixmap;
	}

	boolean 
	checkArgs(String argv[]) //PLATFORM_JAVA
	{
		int argc = argv.length;
		if( argc < 1 ) { 
			System.out.println( "Usage:\t java Decoder" + 
				" image.jpg [l|v|d] [threshold] [scaletype] [scalesize] [windowsize]");
			return false;
		}

		TAG_IMAGE_FILE = argv[0];

		if(argc >= 2){
			String option = argv[1];
			if( option.equals("l") ){
				DEBUG = true;
				System.out.println( "Debug Logging enabled" );
			}
			if( option.equals("v") ){
				VISUAL_DEBUG = true;
				System.out.println( "Visual Debug enabled" );
			}
			if( option.equals("a") ){
				ANCHOR_DEBUG = true;
				System.out.println( "Anchor Debug enabled" );
			}
			if( option.equals("d") ){
				DEBUG = true;
				System.out.println( "Debug Logging enabled" );
				VISUAL_DEBUG = true;
				System.out.println( "Visual Debug enabled" );
			}
		}
		int type = 0;
		if(argc >= 3)                         		  THRESHOLD_OFFSET      = Integer.parseInt(argv[2]);
		if(argc >= 4)                         		  type     		= Integer.parseInt(argv[3]);
		if(argc >= 5) { if(Integer.parseInt(argv[4]) > 0) PIXMAP_SCALE_SIZE     = Integer.parseInt(argv[4]); }
		if(argc >= 6) { if(Integer.parseInt(argv[5]) > 0) THRESHOLD_WINDOW_SIZE = Integer.parseInt(argv[5]); }
		if(type == 2) PIXMAP_NATIVE_SCALE = true;
		if(type == 1) PIXMAP_FAST_SCALE   = false;

		ARGS_OK = true;

		return true;
	}

}

