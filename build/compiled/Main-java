/* 
 * J1.0 Dec 2007 
 */
class Main {
	public static void main(String[] args){
		int[] tag = new int[12];
		Decoder decoder = new Decoder(args);
		if(decoder.processTag()) {
			decoder.copyTag(tag);
			for(int i=0; i<12; i++) System.out.print(tag[i]);
			System.out.println();
		}
		decoder = null;
	}
}
