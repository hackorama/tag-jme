import javax.microedition.lcdui.*;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.*;
import javax.microedition.io.*;
//_MMAPI
import javax.microedition.media.*;
import javax.microedition.media.control.*;
//MMAPI_

import java.io.*;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Thread;
import java.util.Date;

public class MytagoDemo extends MIDlet {

	static final int splashTimeout  = 1;

	Display display;
	MyCanvas canvas;

	boolean once = true;

	public MytagoDemo() {
		display = Display.getDisplay(this);
		canvas = new MyCanvas(this);
	}

	public void startApp() {
        if(once){
			display.setCurrent(canvas);
            once = false;
        }
    }

	public void destroyApp(boolean unconditional) {
    }

    public void pauseApp() {
		if(canvas != null ) canvas.onPauseApp();
    }

	public void exitMIDlet(){
		destroyApp(true);
    	notifyDestroyed();
	}
}

class MyCanvas extends Canvas implements CommandListener {

private static final boolean INTERNALDEMO = false;
	private static final String CAMVIEW_TAGIMAGE = "/camera.jpg";
	private static final String APP_ICON = "/MytagoDemo.png";
	private static final int CAMVIEW_CORRECTION = 20;
	private static final int CAMVIEW_HEIGHT = 160;
	private static final boolean VIDEO_FULLSCREEN = false;
	private static final int SPLASH_MILLISECS  = 3000;
	private static final int FONT_GAP = 2;
	private static final int REPAINT_TICK = 100;
	private static final int DECODING_REPAINT_TICK = 200;
	private int MESSAGE_BAR_HEIGHT = 24;
	private int RESULT_TEXT_BOX_WIDTH = 64; //should be coputed
	private boolean VIDEO_SNAPSHOT = false;
	private boolean dbg_show_perf_data = false;
	private boolean dbg_show_device_data = false;
	private String  dbg_video_status = "V OK";
	private long    dbg_decode_time = 0;

	private boolean SPLASH_SCREEN 	= true;
	private boolean SPLASH_TIMEUP 	= false;
	private boolean READY_SCREEN 	= false;
	private boolean IS_DECODING 	= false;
	private boolean DECODE_STATUS 	= false;
	private boolean DECODE_DONE 	= false;
	private boolean CAMERA_READY 	= false;
	private boolean CAMERA_FAIL		= false;
	private boolean IS_SNAPPING 	= false;
	private boolean SNAP_READY 		= false;
	private boolean SNAP_FAIL 		= false;
	private boolean FAKE_CAMERA 	= false;
	private boolean NO_CAMERA 		= false;
	private boolean CAMERA_STARTING = false;
	private boolean EXIT_SCREEN     = false;

	private boolean EMULATE_VIDEO 	= false;

	private static final Command EXIT_CMD = new Command("Exit", Command.EXIT, 2);
	private static final Command HELP_CMD = new Command("Help", Command.HELP, 3);
	private static final Command OK_CMD = new Command("OK", Command.OK, 1);
	private static final Command GO_CMD   = new Command("Start", Command.ITEM, 1);
	private static final Command SNAP_CMD = new Command("Snap", Command.ITEM, 1);
	private static final Command YES_CMD = new Command("Yes", Command.OK, 1);
	private static final Command NO_CMD = new Command("No", Command.EXIT, 2);
	//private static final Command BACK_CMD = new Command("BACK", Command.BACK, 2);

	//_CLDC11
	private static final String VERSION_ID = "V4TD1";	
	//CLDC11_
	/*CLDC10
	private static final String VERSION_ID = "V4TDE1";	
	CLDC10*/
	private static final String HELPTITLE = "About MInfCode" ;
	private static final String HELPTEXT = "MInfCode Technology Demo " + VERSION_ID  +"\n" 
	+ "Name : MInfCode Demo\n" 
	+ "Vendor: www.hackorama.com/minfcode\n" 
	+ "Version: 1.0\n" 
	+ "Captures MInfCode snapshot using the mobile camera and decodes the code id number. "
	+ "If there is no camera access on the mobile device, uses an emulated code image video for demo.\n"
	+ "More details and updates are at: http://www.hackorama.com/minfcode/";

	private int w = 0, h = 0;
	private int cx = 0, cy = 0;
	private int radius = 0;
	private long splashticks = 0;

	private boolean debug = false;
	private boolean drawonce = false;

	private Random random = new Random();	
	private MytagoDemo midlet;
	private Graphics g = null;
	//created at snap camera thread 
	//disposed in decodeer thread
	private Image snapimage = null; 
	private Image fakeimage = null; 
	private Timer timer = null;
	private int fake_w = 0, fake_h = 0;
	Font font_normal = null, font_bold = null;
	Font font_normal_bold = null, font_small = null, font_small_bold = null;

	TextBox helpText = null;
	Alert helpInfo = null;

	public MyCanvas(MytagoDemo midlet){
		this.midlet = midlet;

    	setCommandListener(this);

		h = getHeight();
		w = getWidth();
		radius = w/4;

		font_normal      = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
		font_normal_bold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
		font_bold        = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
		font_small       = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		font_small_bold  = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

		if(font_normal_bold.getHeight() > 20) MESSAGE_BAR_HEIGHT = font_normal_bold.getHeight()+4;
		RESULT_TEXT_BOX_WIDTH = font_normal_bold.getHeight() + (2*font_small.getHeight()) + (3*FONT_GAP);

		cx =  w/2;
		cy =  (h/2) - MESSAGE_BAR_HEIGHT;

		//_MMAPI
		VIDEO_SNAPSHOT = checkSupportByProperty("video.snapshot.encodings");
		if(debug) System.out.println("MMAPI: Video Capture : " + VIDEO_SNAPSHOT);
		//MMAPI_
	    if(VIDEO_SNAPSHOT == false) dbg_video_status = "NO VSE";
	}

	int[] keys = new int[3];	
	int key_count = 0;
	boolean key_found = false;
	protected void keyPressed(int keyCode){
		//drawDebugTopLeft(getKeyName(keyCode));
		if(key_found) return;
		if(keyCode == KEY_NUM3 ) key_count = 0; //d
		keys[key_count] = keyCode; key_count++;
		if(key_count == 3){
			key_count = 0;
			if(keys[0] == KEY_NUM3 && //d
			   keys[1] == KEY_NUM2 && //b
			   keys[2] == KEY_NUM4 )  //g
			{
				dbg_show_device_data = true;
				dbg_show_perf_data = true;
				key_found = true;
				if(debug) System.out.println("DBG_SHOW_DEVICE_DATA");
			}
			
		}
		if(debug) System.out.println( keyCode + " " + getKeyName(keyCode) );
	}

	public void commandAction(Command c, Displayable d) {
		if (c == HELP_CMD)    	showHelp();
		if (c == OK_CMD)    	closeHelp();
		if (c == Alert.DISMISS_COMMAND)    	closeHelp();
		if (c == GO_CMD)  		startCamera();
		if (c == SNAP_CMD) 		captureCamera();
		if (c == EXIT_CMD)    	confirmExit();
		if (c == YES_CMD)    	exitApp();
		if (c == NO_CMD)    	showReady();
	}

	public void paint(Graphics graphics) {
		if(SPLASH_SCREEN) { //any time taking preloading happens here while we show the startup screen
			g = graphics;
			drawSplash();
			SPLASH_SCREEN = false;
			READY_SCREEN = true;
    		addCommand(EXIT_CMD);
    		addCommand(HELP_CMD);
			EMULATE_VIDEO = VIDEO_SNAPSHOT ? false : true;
		} else if( EXIT_SCREEN ){ //ORDER DEPENDENT 
			drawConfirm();
		} else if(READY_SCREEN) {
			drawSplash();
			drawMessageBar("STARTING", 1);
			if(SPLASH_TIMEUP){
				drawReady(VIDEO_SNAPSHOT);
    			addCommand(GO_CMD);
				READY_SCREEN = false;
			}
		} else if( CAMERA_FAIL ){
			CAMERA_FAIL = false;
			EMULATE_VIDEO = true;
			drawCameraFail();
    		addCommand(GO_CMD);
		} else if( SNAP_READY ){
			SNAP_READY = false;
			removeCommand(GO_CMD);
			decodeTag();
		} else if( IS_SNAPPING ){
			drawSnapping();
		} else if( SNAP_FAIL ){
			SNAP_FAIL = false;
			EMULATE_VIDEO = true;
			drawCameraFail();
    		addCommand(GO_CMD);
		} else if( NO_CAMERA ){
    		removeCommand(GO_CMD);
			drawNoCamera();
		} else if( IS_DECODING ){
			drawDecoding();
		}else if( DECODE_DONE ){
			DECODE_DONE = false;
			drawDecodeDone();
    		addCommand(GO_CMD);
    		addCommand(EXIT_CMD);
			dbg_decode_time = 0;
		} else if( CAMERA_STARTING ){
			drawCameraStart();
		} else if( FAKE_CAMERA ){
			updateFakeCamera();
		}
		if( IS_DECODING ){ //slow down screen refresh (free up CPU) when decoding in progress 
			try { Thread.sleep(DECODING_REPAINT_TICK); }catch(java.lang.InterruptedException e){ }
		}else if(REPAINT_TICK >= 1 ) { 
			try { Thread.sleep(REPAINT_TICK); }catch(java.lang.InterruptedException e){ }
		}
		repaint();
	}

	private void showReady(){
    	removeCommand(YES_CMD);
    	removeCommand(NO_CMD);
		addCommand(EXIT_CMD);
		addCommand(GO_CMD);

		drawReady(VIDEO_SNAPSHOT);
    	addCommand(GO_CMD);
		EXIT_SCREEN = false;
		READY_SCREEN = false;
	}

	private void confirmExit(){
		releaseAppResources();
		addCommand(NO_CMD);
		addCommand(YES_CMD);
		EXIT_SCREEN = true;
		//drawConfirm();
	}

	private void exitApp(){
		EXIT_SCREEN = false;
    	removeCommand(YES_CMD);
    	removeCommand(NO_CMD);
		midlet.exitMIDlet();
	}

	public void onPauseApp(){
		releaseAppResources();
		READY_SCREEN = true;
    	addCommand(EXIT_CMD);
	}

	public void releaseAppResources(){
		releaseCamera();
		resetFlags();
		resetCommands();
	}
	private void resetCommands(){
    	removeCommand(EXIT_CMD);
    	removeCommand(HELP_CMD);
    	removeCommand(GO_CMD);
    	removeCommand(SNAP_CMD);
    	removeCommand(OK_CMD);
		removeCommand(NO_CMD);
		removeCommand(YES_CMD);
	}

	private void resetFlags(){
		//EMULATE_VIDEO  should ot be reset
		//SPLASH SCREEN should be set to false
		SPLASH_SCREEN 	= false;
		READY_SCREEN 	= false;
		IS_DECODING 	= false;
		DECODE_STATUS 	= false;
		DECODE_DONE 	= false;
		CAMERA_READY 	= false;
		IS_SNAPPING 	= false;
		SNAP_READY 		= false;
		SNAP_FAIL 		= false;
		FAKE_CAMERA 	= false;
		NO_CAMERA 		= false;
		CAMERA_STARTING = false;
		EXIT_SCREEN     = false;
	}

	// /*HELP
	private void showHelp(){
		try{
			helpInfo = new Alert(HELPTITLE, HELPTEXT, Image.createImage(APP_ICON), AlertType.INFO);
        }catch(java.io.IOException e){
			showHelpText();
			return;	
		}
		helpInfo.setTimeout(Alert.FOREVER);
    	removeCommand(HELP_CMD);
		helpInfo.setCommandListener(this);
		Display.getDisplay(midlet).setCurrent(helpInfo);
	}

	private void showHelpText(){
		helpText = new TextBox(HELPTITLE, HELPTEXT, HELPTEXT.length(), TextField.UNEDITABLE);
    	removeCommand(HELP_CMD);
		helpText.addCommand(OK_CMD);
		helpText.setCommandListener(this);	
		Display.getDisplay(midlet).setCurrent(helpText);
	}

	private void closeHelp(){
		SPLASH_TIMEUP = true;
		SPLASH_SCREEN = false;
		READY_SCREEN = true;
		Display.getDisplay(midlet).setCurrent(this);
		helpText = null;
	}
	// HELP*/

	// /*CANVAS DRAW
	private void drawSplash(){
		if(timer == null) { 
			timer = new Timer();
			timer.schedule( new SplashTimer(), SPLASH_MILLISECS ); //ONCE
		}
		g.setColor( 255, 255, 255);
		g.fillRect( 0, 0, w, h);
		int o = font_bold.getHeight()+(font_small_bold.getHeight()*2)+(FONT_GAP*3);
		int r = (h - o - MESSAGE_BAR_HEIGHT - (4*FONT_GAP) )/2;
		int a = o + (h-MESSAGE_BAR_HEIGHT-o)/2;
		if( r > w/2 ) r = (w/2)-(2*FONT_GAP);
		drawSplashLogo(a, r);
		//g.setColor( 200, 200, 200);
		//g.fillRect(0,0, w, o);
		g.setColor( 0, 0, 200);
		g.setFont(font_bold);
		int offset = FONT_GAP;
		g.drawString("MYTAGO", w/2, offset, Graphics.TOP | Graphics.HCENTER);
		g.setColor( 0, 0, 0);
		g.setFont(font_small_bold);
		offset+=font_bold.getHeight()+FONT_GAP;
		g.drawString("TECH DEMO [" + VERSION_ID + "]", w/2, offset, Graphics.TOP | Graphics.HCENTER);
		offset+=font_small_bold.getHeight()+FONT_GAP;
		g.drawString("www.mytago.com", w/2, offset, Graphics.TOP | Graphics.HCENTER);
		drawMessageBar("STARTING", 1);
	}

	private void drawReady(boolean cameraok){
		if(INTERNALDEMO) cameraok = true;
	    g.setColor( 255, 255, 255);
		g.fillRect( 0, 0, w, h);
		g.setFont(font_small_bold);
		//drawLogoLight(w, h-MESSAGE_BAR_HEIGHT, w-(2*FONT_GAP));
		if(cameraok){
	    	g.setColor( 0, 175, 0);
			g.drawString("ALL READY", cx, cy-font_small_bold.getHeight()-FONT_GAP, Graphics.TOP | Graphics.HCENTER);
	    	g.setColor( 0, 0, 0);
			g.drawString("TO DECODE TAGS", cx, cy, Graphics.TOP | Graphics.HCENTER);
			g.drawString("USING CAMERA", cx, cy+font_small_bold.getHeight()+FONT_GAP, Graphics.TOP | Graphics.HCENTER);
		}else{
	    	g.setColor( 200, 0, 0);
			g.drawString("NO CAMERA ACCESS", cx, cy-font_small_bold.getHeight()-FONT_GAP, Graphics.TOP | Graphics.HCENTER);
	    	g.setColor( 0, 0, 0);
			g.drawString("USING EMULATED", cx, cy, Graphics.TOP | Graphics.HCENTER);
			g.drawString("CAMERA FOR DEMO", cx, cy+font_small_bold.getHeight()+FONT_GAP, Graphics.TOP | Graphics.HCENTER);
		}
		if(dbg_show_device_data) { 
			drawDebugMemory();
			drawDebugBottom(dbg_video_status);
		}
		drawMessageBar("READY", 0);
    	removeCommand(HELP_CMD);
	}

	private void drawConfirm(){
	    g.setColor( 255, 255, 255);
		g.fillRect( 0, 0, w, h);
		//drawLogoLight(w, h-MESSAGE_BAR_HEIGHT, w-(2*FONT_GAP));
		g.setFont(font_small_bold);
	    g.setColor( 0, 0, 0);
		g.drawString("MYTAGO TECHNOLOGY", cx, cy-font_small_bold.getHeight()-FONT_GAP, Graphics.TOP | Graphics.HCENTER);
		g.drawString("more information at", cx, cy, Graphics.TOP | Graphics.HCENTER);
		g.drawString("www.mytago.com/tech", cx, cy+font_small_bold.getHeight()+FONT_GAP, Graphics.TOP | Graphics.HCENTER);
		drawMessageBar("REALLY EXIT ?", 1);
		if(dbg_show_device_data) { 
			try{
				drawDebugTop(System.getProperty("microedition.configuration") + "/" +
							 System.getProperty("microedition.profiles"));
				drawDebugBottom(System.getProperty( "microedition.platform"));
			}catch(Exception e){} //IGNORE
		}
	}

	private void drawWrappedText(String text, int left_x, int top_y){
		LineEnumeration e = new LineEnumeration(font_small, text, (w-5));
		e.writeTo(g, 5, 5, font_small);		
	}

	private void drawCameraStart(){
	    g.setColor( 0, 0, 0);
		g.fillRect( 0, 0, w, h);
		drawMessageBar("STARTING CAMERA", 1);
	}

	private void drawCameraFail(){
		g.setColor( 255, 255, 255);
		g.fillRect( 0, 0, w, h);
	    g.setColor( 200, 0, 0);
		g.drawString("NO CAMERA ACCESS", cx, cy-font_small_bold.getHeight()-FONT_GAP, Graphics.TOP | Graphics.HCENTER);
	    g.setColor( 0, 0, 0);
		g.drawString("USING EMULATED", cx, cy, Graphics.TOP | Graphics.HCENTER);
		g.drawString("CAMERA FOR DEMO", cx, cy+font_small_bold.getHeight()+FONT_GAP, Graphics.TOP | Graphics.HCENTER);
		drawMessageBar("READY", 0);
	}

	private void drawDecoding(){
		//if(EMULATE_VIDEO) drawOnce();
		if(dbg_show_device_data) drawDebugMemory(true);
		drawMessageBar("DECODING", 1);
		drawLogo(true, (MESSAGE_BAR_HEIGHT/2), h-(MESSAGE_BAR_HEIGHT/2), (MESSAGE_BAR_HEIGHT/2)-FONT_GAP);
	}

	private void drawDecodeDone(){
		drawonce = false;
		if(DECODE_STATUS){
			drawResult(decode_formatted_result);
			drawMessageBar("READY", 0); 
		} else {
			drawResultFail();
			drawMessageBar("READY", 0);
		}
		if(dbg_show_device_data) {
			drawDebugMemory(true);
			String msg = dbg_video_status;	
			if(dbg_show_perf_data){
				long secs = 0;
				if(dbg_decode_time>0) secs = (dbg_decode_time < 1000) ? 1 : dbg_decode_time/1000;
				msg = msg + "," + secs  + "s";
			}
			drawDebugTopExtra(msg);
		}
	}

	private void drawOnce(){
		if(drawonce) return;
		g.setColor( 0, 0, 0);
		g.fillRect( 0, 0, w, h);
		if(EMULATE_VIDEO)  g.drawImage(snapimage, cx, cy, Graphics.VCENTER|Graphics.HCENTER);
		drawonce = true;
	}

	private void drawNoCamera(){
		g.setColor( 255, 255, 255);
		g.fillRect( 0, 0, w, h);
	    g.setColor( 200, 0, 0);
		g.drawString("CANNOT DECODE TAGS", cx, cy-font_small_bold.getHeight()-FONT_GAP, Graphics.TOP | Graphics.HCENTER);
	    g.setColor( 0, 0, 0);
		g.drawString("NO CAMERA ACCESS AND", cx, cy, Graphics.TOP | Graphics.HCENTER);
		g.drawString("NO EMULATED CAMERA", cx, cy+font_small_bold.getHeight()-FONT_GAP, Graphics.TOP | Graphics.HCENTER);
    	removeCommand(SNAP_CMD);
    	removeCommand(GO_CMD);
	}

	private void drawResultFail(){
		//int text_box_width = font_normal_bold.getHeight() + (2*font_small.getHeight()) + (3*FONT_GAP);
		int text_box_width = RESULT_TEXT_BOX_WIDTH;
		int text_box_offset = h - MESSAGE_BAR_HEIGHT - text_box_width;
	    g.setColor( 255, 255, 255);
		g.fillRect( 0, text_box_offset, w, text_box_width);
		//g.fillRect(FONT_GAP, cy-MESSAGE_BAR_HEIGHT, w-(FONT_GAP*2), MESSAGE_BAR_HEIGHT*2);
		g.setColor( 255, 0, 0);
		g.setFont(font_normal_bold);
		text_box_offset+=FONT_GAP;
		g.drawString("PLEASE TRY AGAIN", cx, text_box_offset, Graphics.TOP | Graphics.HCENTER);
		text_box_offset+=(font_normal_bold.getHeight()+FONT_GAP);
		g.setFont(font_small);
		g.setColor( 0, 0, 0);
		g.drawString("EITHER NOT IN FOCUS", cx, text_box_offset, Graphics.TOP | Graphics.HCENTER);
		text_box_offset+=(font_small.getHeight()+FONT_GAP);
		g.drawString("OR NOT A TAG IMAGE", cx, text_box_offset, Graphics.TOP | Graphics.HCENTER);
	}

	private void drawResult(String result){
		int text_box_width = font_small_bold.getHeight() +font_bold.getHeight() + (3*FONT_GAP);
		int text_box_offset = h - MESSAGE_BAR_HEIGHT - text_box_width;
	    g.setColor( 255, 255, 255);
		g.fillRect( 0, text_box_offset, w, text_box_width);
		//g.fillRect(FONT_GAP, cy-MESSAGE_BAR_HEIGHT, w-(FONT_GAP*2), MESSAGE_BAR_HEIGHT*2);
		g.setColor(0, 0, 0);
		g.setFont(font_small_bold);
		text_box_offset+=FONT_GAP;
		g.drawString("DECODED TAG ID", cx, text_box_offset, Graphics.TOP | Graphics.HCENTER);
		text_box_offset+=(font_small_bold.getHeight()+FONT_GAP);
		g.setColor(0, 0, 255);
		g.setFont(font_bold);
		g.drawString(result, cx, text_box_offset, Graphics.TOP | Graphics.HCENTER);
	}

	private void drawSystemProperties(){
		int anchor = 10 + radius + 10;
		g.setColor( 255, 255, 255);
		g.fillRect( 0, 0, w, h);
		drawLogo(false, 10 + radius/2, radius/2);
		g.setColor( 0, 0, 0);
		g.setFont(font_normal);
		Runtime runtime = Runtime.getRuntime();
		//runtime.gc();
		long free = runtime.freeMemory();
		long total = runtime.totalMemory();
		String c = System.getProperty( "microedition.configuration" );	
		String p = System.getProperty( "microedition.platform" );	
		g.drawString(c, w/2, anchor, Graphics.TOP | Graphics.HCENTER);
		anchor+=20;
		g.drawString(p, w/2, anchor, Graphics.TOP | Graphics.HCENTER);
		anchor+=20;
		g.drawString( free + " Free", w/2, anchor, Graphics.TOP | Graphics.HCENTER);
		anchor+=20;
		g.drawString( total + " Total", w/2, anchor, Graphics.TOP | Graphics.HCENTER);
	
	}

	private void drawDebugMemory(){
		drawDebugMemory(false);
	}

	private void drawDebugMemory(boolean background_refresh){
		try{
			Runtime runtime = Runtime.getRuntime();
			long free = runtime.freeMemory();
			long total = runtime.totalMemory();
			if( free > 0 )  free  = (free < 1000)  ? 1 : free/1000;
			if( total > 0 ) total = (total < 1000) ? 1 : total/1000;
			if(background_refresh){
				g.setColor(255, 255, 255);
				g.fillRect( 0, 0, w, font_small.getHeight());
			}
			drawDebugTop(free+"K/"+total+"K");
		}catch(Exception e){ } //IGNORE
	}

	private void drawDebugBottom(String msg){
		g.setColor(255, 0, 0);
		g.setFont(font_small);
		g.drawString(msg, cx, h-MESSAGE_BAR_HEIGHT-font_small.getHeight(), Graphics.TOP | Graphics.HCENTER);
	}

	private void drawDebugTop(String msg){
		g.setColor(255, 0, 0);
		g.setFont(font_small);
		g.drawString(msg, cx, 0, Graphics.TOP | Graphics.HCENTER);
	}

	private void drawDebugTopLeft(String msg){
		g.setColor(0, 0, 0);
		g.fillRect(0, 0, font_small.getHeight(), font_small.getHeight());
		g.setColor(255, 0, 0);
		g.setFont(font_small);
		g.drawString(msg, 0, 0, Graphics.TOP | Graphics.LEFT);
	}

	private void drawDebugTopExtra(String msg){
		g.setColor(255, 0, 0);
		g.setFont(font_small);
		g.drawString(msg, cx, font_small.getHeight()+FONT_GAP, Graphics.TOP | Graphics.HCENTER);
	}

	private void drawMessageBar(String message, int type){
		g.setColor(135, 206, 250);
		g.fillRect(0, h-MESSAGE_BAR_HEIGHT, w, h);
		//g.setColor(0, 0, 200);
		//g.fillRect(0, h-MESSAGE_BAR_HEIGHT, w, 1);
		if(type == 1){
			g.setColor(0, 0, 255);
		}else if(type == 2){
			g.setColor(255, 0, 0);
		}else{
      		g.setColor(0, 0, 0);	
		}
		g.setFont(font_normal_bold);
		int offset = (MESSAGE_BAR_HEIGHT <= font_normal_bold.getHeight()) ? 0 :  
							(MESSAGE_BAR_HEIGHT - font_normal_bold.getHeight())/2;
		g.drawString(message, w/2, h-(MESSAGE_BAR_HEIGHT)+offset, Graphics.TOP | Graphics.HCENTER);
		g.setFont(font_normal);
	}


	private void drawLogo(boolean animated){
		drawLogo(animated, cy, radius);
	}

	private void drawLogo(boolean animated, int _radius){
		drawLogo(animated, cy, _radius);
	}

	private void drawLogo(boolean animated, int yanchor, int _radius){
		drawLogo(animated, cx, yanchor, _radius);
	}

	private void drawLogo(boolean animated, int xanchor, int yanchor, int _radius){
		int r = 0, d = 0;
		if(animated){
			//_CLDC11
      		g.setColor(	random.nextInt(255), 
				   	    random.nextInt(255), 
					    random.nextInt(255));
			//CLDC11_
			/*CLDC10
      		g.setColor(	(random.nextInt() >>> 1) % 255, 
				   	    (random.nextInt() >>> 1) % 255, 
					    (random.nextInt() >>> 1) % 255);
			CLDC10*/
		}else{
			g.setColor( 38, 0, 92);
		}
		r = _radius;
		d = r*2;
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );

		r = r/2 + r/8;
		d = r*2;
		g.setColor( 255, 255, 255);
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );

		r = r - r/4;
		d = r*2;
		g.setColor( 175, 250, 12);
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );
	}

	private void drawLogoLight(int xanchor, int yanchor, int _radius){
		int r = 0, d = 0;
		g.setColor(0xccff99);
		r = _radius;
		d = r*2;
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );

		r = r/2 + r/8;
		d = r*2;
		g.setColor( 255, 255, 255);
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );

		r = r - r/4;
		d = r*2;
		g.setColor(0xccffff);
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );
	}

	int sl_ticks =  (REPAINT_TICK<=1) ? SPLASH_MILLISECS/2 :  (SPLASH_MILLISECS/2)/REPAINT_TICK;
	int sl_r_ticks = 1;
	int sl_g_ticks = 1;
	int sl_b_ticks = 1;
	int sl_r = 255;
	int sl_g = 255;
	int sl_b = 255;
	int sl_max_radius = 0;
	int sl_radius = 1;
	int sl_radius_ticks = -1;
	private void drawSplashLogo(int yanchor, int _radius){
		if( sl_radius_ticks < 0){
			if(sl_ticks > 255-92 ) sl_ticks = 255-92;
			sl_r_ticks = ((255-38)/sl_ticks);
			sl_g_ticks = (255/sl_ticks);
			sl_b_ticks = ((255-92)/sl_ticks);
			sl_max_radius = _radius/2 + _radius/8 - ((_radius/2 + _radius/8)/4);
			sl_radius_ticks = (sl_max_radius / sl_ticks) * 2 ; //go twice faster 
			if(sl_radius_ticks < 1) sl_radius_ticks = 1;
		}
		if(sl_r > 28+sl_r_ticks) sl_r-=sl_r_ticks; 
		if(sl_g >  0+sl_r_ticks) sl_g-=sl_g_ticks; 
		if(sl_b > 92+sl_r_ticks) sl_b-=sl_b_ticks; 
		int xanchor = cx;
		g.setColor( sl_r, sl_g, sl_b);
		int r = 0, d = 0;
		r = _radius;
		d = r*2;
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );

		r = r/2 + r/8;
		d = r*2;
		g.setColor( 255, 255, 255);
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );

		if( sl_radius >= sl_max_radius ){
			sl_radius = sl_max_radius;
		}else{
			sl_radius+=sl_radius_ticks;
		}
		r = sl_radius;
		d = r*2;
		g.setColor( 175, 250, 12);
		g.fillArc( xanchor - r, yanchor - r, d, d, 0, 360 );
	}
	// CANVAD DRAW*/

	// /*CAMERA 
	//_MMAPI 
	private Player player;
	private VideoControl videoControl;
	private VideoCanvas aVideoCanvas;
	String contentType = null;
	//MMAPI_

	private void startCamera(){
		(new CameraThread()).start();
	}

	private void showCamera() {
		if(EMULATE_VIDEO) showFakeCamera();
		else		      showRealCamera();
	}

	private void showRealCamera(){
		CAMERA_STARTING = true;
		//_MMAPI 
		try {
			releaseResources();
			player = Manager.createPlayer("capture://video");
			player.realize();

			videoControl = (VideoControl)player.getControl("VideoControl");
			aVideoCanvas = new VideoCanvas();
			if( aVideoCanvas.initControls(videoControl, player) ){

				aVideoCanvas.addCommand(SNAP_CMD);
				aVideoCanvas.addCommand(EXIT_CMD);
				aVideoCanvas.setCommandListener(this);

				Display.getDisplay(midlet).setCurrent(aVideoCanvas);

				player.start();
				contentType = player.getContentType();


				CAMERA_STARTING = false;
				CAMERA_READY = true;
			}else{
				CAMERA_FAIL = true;
			}
		}catch(Exception e) {
			if(debug) System.out.println("SHOWCAMERA: " + e.getMessage() );
			CAMERA_FAIL = true;
	    	dbg_video_status = "VCP VC EX";
		}
		//MMAPI_
		CAMERA_STARTING = false;
	}

	private void showFakeCamera(){
		removeCommand(GO_CMD);
		CAMERA_STARTING = true;
		g.setColor( 255, 255, 255);
		g.fillRect( 0, 0, w, h);
		try{
          	fakeimage = Image.createImage(CAMVIEW_TAGIMAGE);
			fake_w = fakeimage.getWidth();
			fake_h = fakeimage.getHeight();
			//g.drawImage(fakeimage, w/2, (h/2)-MESSAGE_BAR_HEIGHT, g.VCENTER|g.HCENTER);
			FAKE_CAMERA = true;
        }catch(java.io.IOException e){
			if(debug) System.out.println( "showFakeCamera: " + e.getMessage() );
			FAKE_CAMERA = false;
			NO_CAMERA = true;
		}
		addCommand(SNAP_CMD);
		addCommand(EXIT_CMD);
		fakeCameraMessage();
		CAMERA_STARTING = false;
	}

	private void updateFakeCamera(){
		if(!FAKE_CAMERA) return;
		int CAMSHAKE=3;
		//_CLDC11
		int dx = random.nextInt(CAMSHAKE*2);		
		int dy = random.nextInt(CAMSHAKE*2);
		//CLDC11_
		/*CLDC10
		int dx = (random.nextInt() >>> 1) % (CAMSHAKE*2);
		int dy = (random.nextInt() >>> 1) % (CAMSHAKE*2);
		CLDC10*/
		int n_cy = (h/2);
		if(CAMVIEW_HEIGHT > h) n_cy+=CAMVIEW_CORRECTION;
		g.setColor( 0, 0, 0);
		g.fillRect( 0, 0, w, h);
		g.drawImage(fakeimage, cx-CAMSHAKE+dx, n_cy-CAMSHAKE+dy, Graphics.VCENTER|Graphics.HCENTER);
		if(fake_h < (h+(CAMSHAKE*2))){
			g.fillRect(0, n_cy-(fake_h/2)-CAMSHAKE, w, CAMSHAKE*2);
			g.fillRect(0, n_cy+(fake_h/2)-CAMSHAKE, w, CAMSHAKE*2);
		}
		if(fake_w < (w+(CAMSHAKE*2))){
			g.fillRect(cx-(fake_w/2)-CAMSHAKE, 0, CAMSHAKE*2, h);
			g.fillRect(cx+(fake_w/2)-CAMSHAKE, 0, CAMSHAKE*2, h);
		}
		fakeCameraMessage();
	}

	private void fakeCameraMessage(){
		/*g.setColor( 0, 255, 0);
		g.fillRect( 0, h-font_small.getHeight()+(2*FONT_GAP), w, font_small.getHeight()+(2*FONT_GAP));*/
		g.setColor( 0, 255, 0);
		g.setFont(font_small);
		if(!INTERNALDEMO) g.drawString("EMULATED CAMERA", w/2, h-(font_small.getHeight()+FONT_GAP), Graphics.TOP | Graphics.HCENTER);
		g.setFont(font_normal);
	}

	private void releaseCamera() {
		releaseResources();
		Display.getDisplay(midlet).setCurrent(this);
	}

	private void stopCamera() {
    	addCommand(GO_CMD);
    	addCommand(EXIT_CMD);
		releaseResources();
		Display.getDisplay(midlet).setCurrent(this);
		drawReady(VIDEO_SNAPSHOT);
	}

	private void drawSnapping() {
		if(dbg_show_device_data) drawDebugMemory(true);
		drawMessageBar("CAPTURING", 1);
	}

	private void captureCamera() {
		IS_SNAPPING = true;
    	removeCommand(SNAP_CMD);
    	removeCommand(GO_CMD);
		(new CaptureThread()).start();
	}

	private void snapCamera() { //called from CaptureThread
		if(EMULATE_VIDEO) snapFakeCamera();
		else		 	  snapRealCamera();
	}

	private void snapRealCamera() {
	    //_MMAPI 
		try {
			g.setColor(0, 0, 0);
			g.fillRect(0, 0, w, h);
			// "encoding=jpeg&width=320&height=240"
			byte[] raw = videoControl.getSnapshot( null ); 
			snapimage = Image.createImage(raw, 0, raw.length);
			SNAP_READY = true;
	    	dbg_video_status = "VC " + snapimage.getWidth() + "x" + snapimage.getHeight();
    	} catch( Exception e ) { 
			if(debug) System.out.println("CAPTURECAMERA: " + e.getMessage() );
			SNAP_FAIL =true;
			VIDEO_SNAPSHOT=false;
	    	dbg_video_status = "VC GS EX";
		}
		if(debug) System.out.println("Capture Snapshot");
		releaseCamera();
		IS_SNAPPING = false;
	    //MMAPI_
	}

	private void snapFakeCamera() {
		FAKE_CAMERA = false;
		snapimage = fakeimage;
		SNAP_READY = true;
		IS_SNAPPING = false;
	}

	private void releaseResources() {
	    //_MMAPI 
		if ( player != null ) {
			try {
				player.stop();
				player.close();
			}catch(Exception e){}
		}
	    //MMAPI_
	}
	// CAMERA*/

	// /*DECODE
	private int[] tag = new int[12];
	private String decode_actual_result = null;
	private String decode_formatted_result = null;

	private void decodeTag() {
		(new DecodeThread()).start();
	}

	public void startDecode() {
		IS_DECODING = true;
		Date date = new Date();
		long starttime = 0;
		if(dbg_show_perf_data) starttime = date.getTime();
		try{
			Tagimage tagimage  = new Tagimage(snapimage);
			Decoder decoder = null;
			if(EMULATE_VIDEO) decoder    = new Decoder(tagimage, false);//Keep Image
			else 			  decoder    = new Decoder(tagimage, true); //Delete Image
			Config config = decoder.getConfig();
			if(EMULATE_VIDEO) config.PIXMAP_SCALE_SIZE = 240;//Match the stored demo image size
			config.DEBUG = debug;
			//g.drawImage(tagimage.getImage(), cx, cy, g.VCENTER|g.HCENTER);
			//int size = (w < (h-MESSAGE_BAR_HEIGHT)) ? w : (h-MESSAGE_BAR_HEIGHT);
			tagimage.drawImage(g, w, h, h-MESSAGE_BAR_HEIGHT-RESULT_TEXT_BOX_WIDTH); 
			if(EXIT_SCREEN) {
				decoder.dispose();
        		decoder = null;
				return;
			}
        	if(decoder.processTag()) {
				if(EXIT_SCREEN) {
					decoder.dispose();
        			decoder = null;
					return;
				}
				tagimage = null;
				decoder.copyTag(tag);
				decode_actual_result = "";
				decode_formatted_result = "";
				DECODE_STATUS = true;
				boolean tagresult = true;
            	for(int i=0; i<12; i++) { 
					decode_formatted_result = decode_formatted_result + Integer.toString(tag[i]);
					decode_actual_result    = decode_actual_result + Integer.toString(tag[i]);
					if(i == 3 || i == 7) decode_formatted_result = decode_formatted_result + " ";
					if( tag[i] < 0 ) tagresult = false;
				}
				if(EXIT_SCREEN) {
					decoder.dispose();
        			decoder = null;
					return;
				}
				DECODE_STATUS = tagresult;
            	if(debug) System.out.println(decode_formatted_result);
        	}else{
				if(debug) System.out.println("FAIL");
				decode_formatted_result = "";
			}
			decoder.dispose();
        	decoder = null;
		}catch(Exception e){ 
			if(debug) System.out.println( "DECODE : " + e.getMessage() ); 
			e.printStackTrace();
		}
		if(EXIT_SCREEN) return;
		IS_DECODING = false;
		DECODE_DONE = true;
		date = new Date();
		if(dbg_show_perf_data) dbg_decode_time = date.getTime() - starttime;
	}
	
	// DECODE*/

	// /*TASK THREADS
	class CameraThread extends Thread {
		public void run() {
			showCamera();
		}
	}

	class CaptureThread extends Thread {
		public void run() {
			snapCamera();
		}
	}

	class DecodeThread extends Thread {
		public void run() {
			startDecode();
		}
	}

	private class SplashTimer extends TimerTask {
        public void run(){
			SPLASH_TIMEUP = true;
        }
    }
	// TASK THREADS*/

	//"microedition.io.file.FileConnection.version"
	private boolean checkSupportByProperty(String property){
		String support = System.getProperty( property );
		if(support == null)       return false;
		if(support.length() <= 0) return false;
		return true;
	}

	//javax.wireless.messaging.MessageConnection
	private boolean checkSupportByClass(String classname){
		try{
			Class.forName(classname);
        	return true;
		}catch(Exception e ){
       		return false;
    	}
	}
}

//_MMAPI 
class VideoCanvas  extends Canvas {
	private static final boolean VIDEO_FULLSCREEN = false;
	private String  dbg_video_status = null;
	boolean debug = false;
	boolean once = true;
	int w = 0, h = 0;

	public boolean initControls(VideoControl videoControl, Player player) {
		w = getWidth();
		h = getHeight();
		try { 
			videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
			if(VIDEO_FULLSCREEN){
				try { 
					videoControl.setDisplayFullScreen(true); 
				} catch (MediaException me) { 
					if(debug) System.out.println("initControls ME: " + me.getMessage() );
	    	    	dbg_video_status = "VC IDM EX";
					return false; 
				}
			}else{
				try {
					videoControl.setDisplayLocation(0, 0);
					videoControl.setDisplaySize(w, h);
				} catch (MediaException me1) {
					try { 
						videoControl.setDisplayFullScreen(true); 
					} catch (MediaException me2) { 
						if(debug) System.out.println("initControls ME: " + me2.getMessage() );
	    	    	    dbg_video_status = "VC IDM EX";
						return false;
					}
				}
			}
			videoControl.setVisible(true);
		} catch (Exception e) { 
			if(debug) System.out.println("initControls: " + e.getMessage() );
	    	dbg_video_status = "VC IC EX";
			return false; 
		}
		return true;
	}

	public void paint(Graphics g) {
		if(once){
			once = false;
			g.setColor(0, 0, 0);
			g.fillRect(0, 0, w, h);
			//g.setColor(0, 255, 0);
			//g.drawRect(4, 4, w-8, h-8);
		}
	}

	public String getDebugVideoStatus(){
		return dbg_video_status;
	}
}
//MMAPI_
