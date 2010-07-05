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
 * The CommandThread class is designed to move all command processing into a 
 * single thread to be run in the background and avoid conflicts between 
 * threads.  Commands are processed in the order that they were queued.  The 
 * arguements for each command are stored in a 
 * {@link paulscode.sound.CommandObject CommandObject}.  The Command Queue is 
 * located in the {@link paulscode.sound.SoundSystem SoundSystem} class.  
 * Calling kill() stops the thread, and this should be immediatly followed 
 * by a call to interrupt() to wake up the thread so it may end.  This class 
 * also checks for temporary sources that are finished playing, and removes 
 * them.  
 * NOTE: The command thread is created automatically by the sound system, so it 
 * is unlikely that the user would ever need to use this class.
 * 
 * Author: Paul Lamb
 */
public class CommandThread extends SimpleThread
{
/**
 * Processes status messages, warnings, and error messages.
 */
    protected SoundSystemLogger logger;
    
/**
 * Handle to the Sound System.  This is where the Command Queue is located.
 */
    private SoundSystem soundSystem;
    
/**
 * Name of this class.
 */
    protected String className = "CommandThread";
    
/**
 * Constructor:  Takes a handle to the SoundSystem object as a parameter.
 * @param s Handle to the SoundSystem.
*/
    public CommandThread( SoundSystem s )
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        soundSystem = s;
    }
    
/**
 * Shuts the thread down and removes references to all instantiated objects.  
 * NOTE: Method alive() will return false when cleanup() has finished.
 */
    @Override
    protected void cleanup()
    {
        kill();
        
        logger = null;
        soundSystem = null;
        
        super.cleanup();  // Important!
    }
    
/**
 * The main loop for processing commands.  The Command Thread starts out 
 * asleep, and it sleeps again after it finishes processing commands, so it 
 * must be interrupted when commands are queued for processing.
 */
    @Override
    public void run()
    {
        long previousTime = System.currentTimeMillis();
        long currentTime = previousTime;
        
        if( soundSystem == null )
        {
            errorMessage( "SoundSystem was null in method run().", 0 );
            cleanup();
            return;
        }
        
        // Start out asleep:
        snooze( 3600000 );
        
        while( !dying() )
        {
            // Process all queued cull and activate commands:
            soundSystem.ManageSources( null );
            
            // Process all queued commands:
            soundSystem.CommandQueue( null );
            
            // Remove temporary sources every ten seconds:
            currentTime = System.currentTimeMillis();
            if( (!dying()) && ((currentTime - previousTime) > 10000) )
            {
                previousTime = currentTime;
                soundSystem.removeTemporarySources();
            }
            
            // Wait for more commands:
            if( !dying() )
                snooze( 3600000 );
        }
        
        cleanup();   // Important!
    }
    
/**
 * Prints a message.
 * @param message Message to print.
 */
    protected void message( String message, int indent )
    {
        logger.message( message, indent );
    }
    
/**
 * Prints an important message.
 * @param message Message to print.
 */
    protected void importantMessage( String message, int indent )
    {
        logger.importantMessage( message, indent );
    }
    
/**
 * Prints the specified message if error is true.
 * @param error True or False.
 * @param message Message to print if error is true.
 * @return True if error is true.
 */
    protected boolean errorCheck( boolean error, String message )
    {
        return logger.errorCheck( error, className, message, 0 );
    }
    
/**
 * Prints an error message.
 * @param message Message to print.
 */
    protected void errorMessage( String message, int indent )
    {
        logger.errorMessage( className, message, indent );
    }
}
