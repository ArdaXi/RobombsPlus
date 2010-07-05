package robombs.game.util;

/**
 * A simple ticker class for measuring in-game-time. This was formerly based on the LWJGL Timer class, but that class
 * has some unfixed problems on some machines/VMs...so it's based on Java5's nanotime now.
 */
public class Ticker {

	private int rate;
    private long s2;

    public static long getTime() {
    	return System.nanoTime()/1000000L;
    }
    
    /**
     * Create a new ticker that ticks every n-th millisecond.
     * @param tickrateMS the interval
     */
    public Ticker(int tickrateMS) {
        rate = tickrateMS;
        s2 = System.nanoTime()/1000000L;
    }

    /**
     * Reset the ticker.
     */
    public void reset() {
        s2=System.nanoTime()/1000000L;
    }

    /**
     * Forward the ticker to the next tick.
     */
    public void forward() {
        s2=System.nanoTime()/1000000L-rate;
    }
    
    public static boolean hasPassed(long startTime, long time) {
    	 long dif=Ticker.getTime()-startTime; 
         if (dif<0) { 
                 return true;    
         } 
         return dif>time;
    }
    
    public static boolean hasNotPassed(long startTime, long time) {
   	 long dif=Ticker.getTime()-startTime; 
        if (dif<0) { 
                return true;    
        } 
        return dif<time;
   }

    /**
     * How many ticks have passed since the last call?
     * @return int number of ticks
     */
    public int getTicks() {
        long i = System.nanoTime()/1000000L;
        if (i - s2 > rate) {
            int ticks = (int) ((i - s2) / (long) rate);
            s2 += (long)rate * ticks;
            return ticks;
        }
        return 0;
    }
}
