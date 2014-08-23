class Decoder
{

	private Config config;
	private int[] tag;
	private Tagimage tagimage = null;
	private boolean deleteimage = false;

	Decoder(String _filename)
	{
		init();
		config.TAG_IMAGE_FILE = _filename;
		config.ARGS_OK = true;
	}

	Decoder(String[] args)
	{
		init();
		config.checkArgs(args);
	}

	Decoder(Tagimage _tagimage)
	{
		init();
		tagimage = _tagimage;
		deleteimage = false;
		if(tagimage != null) config.ARGS_OK = true;
	}

	Decoder(Tagimage _tagimage, boolean _deleteimage)
	{
		init();
		tagimage    = _tagimage;
		if(tagimage != null) config.ARGS_OK = true;
		deleteimage = _deleteimage;
	}

	void
	dispose()
	{
		config.dispose();
		config = null;
		if(deleteimage && tagimage!= null ) tagimage = null;
	}

	void
	init()
	{
		tag = new int[12];
		for(int i=0; i<12; i++) tag[i] = -1;
		config = new Config();
	}

	Config
	getConfig()
	{
		return config;
	}

	void
	copyTag(int[] _tag)
	{
		for(int i=0; i<12; i++) _tag[i] = tag[i];
	}

	boolean
	processTag()
	{
		if(!config.ARGS_OK ) return false;
		if(tagimage == null) tagimage = new Tagimage(config);
		if(!tagimage.isValid()) { 
			if(deleteimage) { 
				tagimage.dispose();
				tagimage = null;
			}
			return false;
		}
		//if(config.VISUAL_DEBUG) config.setDebugPixmap(new Pixmap(config.TAG_IMAGE_FILE));
		Threshold threshold = new Threshold(config, tagimage);
		threshold.computeEdgemap();
		if(deleteimage) { 
			tagimage.dispose();
			tagimage = null;
		}
		threshold = null;
		Shape[] shapes = new Shape[config.MAX_SHAPES];
		for(int i=0; i < config.MAX_SHAPES; i++) shapes[i] = new Shape(); //JONLY
		Shape anchor = new Shape(config);
		Border border = new Border(config, shapes, anchor);
		int nshapes = border.findShapes();
		border = null;
		if( nshapes >= 12  ){
			Pattern pattern = new Pattern(config, shapes, nshapes, anchor);
			pattern.findCode(tag);
			pattern = null;
		}
		anchor = null;
		shapes = null;
		return true;
	}
}
