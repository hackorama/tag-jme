/*
LICENCE TO USE THE MERRY NEWTON MOBILE UTILITIES

MNMU 1.0 is covered by a version of the BSD licence. 
Read this licence before using the files.

Copyright (c) 2006, Merry-Newton.com
All rights reserved.

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, 
  this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, 
  this list of conditions and the following disclaimer in the documentation 
  and/or other materials provided with the distribution.
* Neither the name of Merry-Newton.com nor the names of its contributors 
  may be used to endorse or promote products derived from this software 
  without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
*/

/*
PLEASE REPORT BUGS TO <bugs@merry-newton.com>
*/
public class IntMath{
	private static final short[] quarterSineTable={(short)0,(short)1608,(short)3215,(short)4821,(short)6423,(short)8022,(short)9616,(short)11204,(short)12785,(short)14359,(short)15923,(short)17479,(short)19024,(short)20557,(short)22078,(short)23586,(short)25079,(short)26557,(short)28020,(short)29465,(short)30893,(short)32302,(short)-31844,(short)-30475,(short)-29127,(short)-27800,(short)-26497,(short)-25217,(short)-23961,(short)-22730,(short)-21525,(short)-20347,(short)-19196,(short)-18072,(short)-16978,(short)-15912,(short)-14876,(short)-13871,(short)-12897,(short)-11955,(short)-11045,(short)-10168,(short)-9324,(short)-8514,(short)-7739,(short)-6998,(short)-6293,(short)-5623,(short)-4989,(short)-4392,(short)-3831,(short)-3308,(short)-2822,(short)-2374,(short)-1965,(short)-1593,(short)-1260,(short)-965,(short)-710,(short)-493,(short)-316,(short)-178,(short)-79,(short)-20};

	private IntMath(){}

	public static int sin(int theta){
		// average of two table entries
		int proportion=theta&0xff;
		theta>>>=8;
		int theta2=theta+1;
		// so now: theta = 00000000xxxxxxxxxxxxxxxxqqiiiiii
		// this applies to both
		// now reuse theta as result 1
		theta=sinLookup((theta>>>6)&3,theta&63);
		theta2=sinLookup((theta2>>>6)&3,theta2&63);

		return (proportion*theta2+(256-proportion)*theta)>>8;
	}

	public static int cos(int theta){
		return sin(theta+0x4000);
	}

	private static int sinLookup(int quad,int index){
		int r;
		if((quad&1)==0){
			// read table forward
			r=0x0000ffff&quarterSineTable[index]; // the mask is because vm will make sign-true widening conversion, and I don't want that
		}else{
			// read backward
			if(index==0){
				r=0x00010000;
			}else{
				r=0x0000ffff&quarterSineTable[64-index];
			}
		}
		// quad&2 is 0 if pos half, 2 if neg
		return r*(1-(quad&2));
	}

	public static int sqr(int n){
		// there is probably a more efficient way
		boolean neg = (n<0);
		if(neg) n=-n;
		// note: B504 is the highest rounded-down answer
		int highAttempt	=	0xb507;
		int lowAttempt	=	0;
		int attempt;
		int result;
		do{
			attempt=(highAttempt+lowAttempt)/2;
			result=attempt*attempt;
			if(result>n){
				highAttempt=attempt;
			}else{
				lowAttempt=attempt;
			}
		}while(highAttempt-lowAttempt >1);
		return (neg)?-lowAttempt:lowAttempt;
	}

	public static long sqr(long n){
		boolean neg = (n<0L);
		if(neg) n=-n;
		long highAttempt	=	0xb504f335L; // this should work now that an "L" has been added
		long lowAttempt	=	0;
		long attempt;
		long result;
		do{
			attempt=(highAttempt+lowAttempt)/2;
			result=attempt*attempt;
			if(result>n){
				highAttempt=attempt;
			}else{
				lowAttempt=attempt;
			}
		}while(highAttempt-lowAttempt >1);
		return (neg)?-lowAttempt:lowAttempt;
	}

	// RAISE THE FIRST ARGUMENT TO THE POWER OF THE SECOND
	// b will be treated as an UNSIGNED integer
	public static long power(int x,int y){
		long p=1;
		long b=((long)y)&0x00000000ffffffffL;
		// bits in b correspond to values of powerN
		// so start with p=1, and for each set bit in b, multiply corresponding table entry
		long powerN=x;
		while(b!=0){
			if((b&1)!=0) p*=powerN;
			b>>>=1;
			powerN=powerN*powerN;
		}
		return p;  // YOU HAVE NOT FORGOTTEN ZERO: it just isn't a special case
	}

	public static int tan(int theta){
		return ( sin(theta)/cos(theta) );
	}

	public static int abs(int n){
		if(n<0) n*=-1;
		return n;
	}

} 
