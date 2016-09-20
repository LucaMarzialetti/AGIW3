package it.uniroma3.agiw3.support;

import java.util.Random;

public class SleepTime {
	public static long calc(int min, int max){
		Random r = new Random();
		return (long) (r.nextInt(max-min)+min);
	}
}
