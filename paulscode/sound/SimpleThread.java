//    You are free to use this library for any purpose, commercial or otherwise.
//    You can alter the code, and/or distribute it any way you like.
// 
//    If you change the code, please document the changes made before
//    redistributing it, so other users know it is not the original code.
// 
//    You are not required to give me credit, but it would be nice :)
// 
//    Author: Paul Lamb
//    http://www.paulscode.com
package paulscode.sound;


/**
 * The SimpleThread class is the template used to create all thread classes 
 * used by in the SoundSystem library.  It provides methods for common actions 
 * like sleeping, killing, and checking liveness.  NOTE: super.cleanup() must 
 * be called at the bottom of overriden cleanup() methods, and cleanup()
 * must be called at the bottom of the run() method for all extended classes.  
 * 
 * Author: Paul Lamb
 */
public class SimpleThread extends Thread
{
/**
 * Used to return a current value from one of the synchronized 
 * boolean-interface methods.
 */
    private static final boolean GET = false;
    
/**
 * Used to set the value in one of the synchronized boolean-interface methods.
 */
    private static final boolean SET = true;
    
/**
 * Used when a parameter for one of the synchronized boolean-interface methods 
 * is not aplicable.
 */
    private static final boolean XXX = false;
    
/**
 * True when thread is running.
 */
    private boolean alive = true;
    
/**
 * True when thread should end.
 */
    private boolean kill = false;
    
/**
 * Removes all references to instantiated objects, and changes the thread's 
 * state to "not alive".  Method alive() returns false when this method has 
 * completed.  NOTE: super.cleanup() must be called at the bottom of overriden 
 * cleanup() methods, and cleanup() must be called at the bottom of the run() 
 * method for all extended classes.
 */
    protected void cleanup()
    {
        kill( SET, true );  // tread needs to shut down
        alive( SET, false );  // thread has ended
    }
    
/**
 * Executes the thread's main loop.  NOTES: Extended classes should check 
 * method dying() often to know when the user wants the thread to shut down.  
 * Method cleanup() must be called at the bottom of the run() method for all 
 * extended classes.
 */
    @Override
    public void run()
    {
        /*  How the run() method should be set up:  */
        
        
        // Do your stuff here.  Remember to check dying() often to know when 
        // the user wants the thread to shut down.
        
        // MUST call cleanup() at the bottom of Overridden run() method!!!!!
        cleanup();  // clears memory and sets status to dead.
    }
    
/**
 * Calls the rerun() method on a seperate thread, which calls run() when the 
 * previous thread finishes.
 */
    public void restart()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                rerun();
            }
        }.start();
    }

/**
 * Kills the previous thread, waits for it to die, then calls run().
 */
    private void rerun()
    {
        kill( SET, true );
        while( alive( GET, XXX ) )
        {
            snooze( 100 );
        }
        alive( SET, true );
        kill( SET, false );
        run();
    }
    
/**
 * Returns true when the cleanup() method has finished.  This method should be 
 * used to know when the thread has been safely shut down.
 * @return True while the thread is alive.
 */
    public boolean alive()
    {
        return alive( GET, XXX );
    }
    
/**
 * Causes method dying() to return true, letting the thread know it needs to 
 * shut down.
 */
    public void kill()
    {
        kill( SET, true );
    }
    
/**
 * Returns true when the thread is supposed to shut down.
 * @return True if the thread should die.
 */
    protected boolean dying()
    {
        return kill( GET, XXX );
    }
    
/**
 * Sets or returns the value of boolean 'alive'.
 * @param action GET or SET.
 * @param value New value if action == SET, or XXX if action == GET.
 * @return True while the thread is alive.
 */
    private synchronized boolean alive( boolean action, boolean value )
    {
        if( action == SET )
            alive = value;
        return alive;
    }
    
/**
 * Sets or returns the value of boolean 'kill'.
 * @param action GET or SET.
 * @param value New value if action == SET, or XXX if action == GET.
 * @return True if the thread should die.
 */
    private synchronized boolean kill( boolean action, boolean value )
    {
        if( action == SET )
            kill = value;
        return kill;
    }
    
/**
 * Sleeps for the specified number of milliseconds.
 */
    protected void snooze( long milliseconds )
    {
        try
        {
            Thread.sleep( milliseconds );
        }
        catch( InterruptedException e ){}
    }    
}
