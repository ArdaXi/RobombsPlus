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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The SoundSystem class is the main class of the SoundSystem library.  
 * It is designed to create an easy interface for using more than one sound
 * library in a program.  It provides high-level commands that hide all the
 * messy work involved with each individual sound library.  There should be 
 * only one instance of this class in the program!  The SoundSystem can be 
 * constructed by defining which sound library to use, or by using the default 
 * sound library.  See 
 * {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more 
 * information about changing default settings.
 * 
 * Author: Paul Lamb
 */
public class SoundSystem
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
 * Processes status messages, warnings, and error messages.
 */
    protected SoundSystemLogger logger;
    
/**
 * Handle to the active sound library.
 */
    protected Library soundLibrary;
    
/**
 * List of queued commands to perform.
 */
    protected List<CommandObject> commandQueue;
    
/**
 * List of sources that need to be culled or reactivated.
 */
    protected List<CommandObject> sourceManagementList;
    
/**
 * Processes queued commands in the background.
 */
    protected CommandThread commandThread;    
    
/**
 * Generates random numbers.
 */
    public Random randomNumberGenerator;
    
/**
 * Name of this class.
 */
    protected String className = "SoundSystem";
    
/**
 * Indicates the currently loaded sound-library, or -1 if none.
 */
    private static int currentLibrary = -1;
    
/**
 * Becomes true when the sound library has been initialized.
 */
    private static boolean initialized = false;
    
/**
 * Indicates the last exception that was thrown.
 */
    private static SoundSystemException lastException = null;
    
/**
 * Constructor: Create the sound system using the default library.  If the 
 * default library is not compatible, another library type will be loaded 
 * instead, in the order of library preference.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for 
 * information about sound library types.
 */
    public SoundSystem()
    {
        try
        {
            init( SoundSystemConfig.getDefaultLibrary() );
            return;
        }
        catch( SoundSystemException sse )
        {}
        
        int[] libs = SoundSystemConfig.getLibraryPriorities();
        
        for( int x = 0; x < libs.length; x++ )
        {
            if( libs[x] != SoundSystemConfig.getDefaultLibrary() )
            {
                try
                {
                    init( libs[x] );
                    return;
                }
                catch( SoundSystemException sse )
                {}
            }
        }
    }
    
/**
 * Constructor: Create the sound system using the specified library.  
 * @param library Library to use.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for
 * information about chosing a sound library.
 */
    public SoundSystem( int library ) throws SoundSystemException
    {
        try
        {
            init( library );
        }
        catch( SoundSystemException sse )
        {}
    }
    
/**
 * Loads the message logger, initializes the specified sound library, and
 * starts the command thread.  Also instantiates the random number generator 
 * and the command queue.
 * @param library Library to initialize.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for
 * information about chosing a sound library.
 */
    protected void init( int library ) throws SoundSystemException
    {
        // create the message logger:
        logger = SoundSystemConfig.getLogger();
        // if the user didn't create one, then do it now:
        if( logger == null )
        {
            logger = new SoundSystemLogger();
            SoundSystemConfig.setLogger( logger );
        }
        
        message( "", 0 );
        message( "Starting up " + className + "...", 0 );
        
        // create the random number generator:
        randomNumberGenerator = new Random();
        // create the command queue:
        commandQueue = new LinkedList<CommandObject>();
        // create the source management list:
        sourceManagementList = new LinkedList<CommandObject>();
        
        // Instantiate and start the Command Processer thread:
        commandThread = new CommandThread( this ); // Gets a SoundSystem handle
        commandThread.start();
        
        newLibrary( library );
        message( "", 0 );
    }
    
/**
 * Ends the command thread, shuts down the sound system, and removes references 
 * to all instantiated objects.
 */
    public void cleanup()
    {
        boolean killException = false;
        message( "", 0 );
        message( className + " shutting down...", 0 );
        
        // End the command thread:
        try
        {
            commandThread.kill();        // end the command processor loop.
            commandThread.interrupt();   // wake the thread up so it can end.
        }
        catch( Exception e )
        {
            killException = true;
        }
        
        if( !killException )
        {
            // wait up to 5 seconds for command thread to end:
            for( int i = 0; i < 50; i++ )
            {
                if( !commandThread.alive() )
                    break;
                try
                {
                    Thread.sleep(100);
                }
                catch(Exception e)
                {}
            }
        }
        
        // Let user know if there was a problem ending the command thread
        if( killException || commandThread.alive() )
        {
            errorMessage( "Command thread did not die!", 0 );
            message( "Ignoring errors... continuing clean-up.", 0 );
        }
        
        initialized( SET, false );
        currentLibrary( SET, -1 );
        try
        {
            // Stop all sources and shut down the sound library:
            if( soundLibrary != null )
                soundLibrary.cleanup();
        }
        catch( Exception e )
        {
            errorMessage( "Problem during Library.cleanup()!", 0 );
            message( "Ignoring errors... continuing clean-up.", 0 );
        }
        
        try
        {
            // remove any queued commands:
            if( commandQueue != null )
                commandQueue.clear();
        }
        catch( Exception e )
        {
            errorMessage( "Unable to clear the command queue!", 0 );
            message( "Ignoring errors... continuing clean-up.", 0 );
        }
        
        try
        {
            // empty the source management list:
            if( sourceManagementList != null )
                sourceManagementList.clear();
        }
        catch( Exception e )
        {
            errorMessage( "Unable to clear the source management list!", 0 );
            message( "Ignoring errors... continuing clean-up.", 0 );
        }
        
        // Remove references to all instantiated objects:
        randomNumberGenerator = null;
        soundLibrary = null;
        commandQueue = null;
        sourceManagementList = null;
        commandThread = null;
        
        importantMessage( "Author: Paul Lamb, www.paulscode.com", 1 );
        message( "", 0 );
    }
    
/**
 * Pre-loads a sound into memory.  The file may either be located within the 
 * JAR or at an online location.  If the file is online, filename must begin 
 * with "http://", since that is how SoundSystem recognizes URL's.  If the file 
 * is located within the compiled JAR, the package in which sound files are 
 * located may be set by calling SoundSystemConfig.setSoundFilesPackage().
 * @param filename Sound file to load.
 */
    public void loadSound( String filename )
    {
        // Queue a command to load the sound file:
        CommandQueue( new CommandObject( CommandObject.LOAD_SOUND, filename ) );
        // Wake the command thread to process commands:
        commandThread.interrupt();
    }
    
/**
 * Removes a pre-loaded sound from memory.  This is a good method to use for 
 * freeing up memory after a large sound file is no longer needed.  NOTE: the 
 * source will remain in memory after calling this method as long as the 
 * sound is attached to an existing source.  After calling this method, calls 
 * should also be made to method removeSource( String ) for all sources which 
 * this sound is bound to.
 * @param filename Sound file to unload.
 */
    public void unloadSound( String filename )
    {
        // Queue a command to load the sound file:
        CommandQueue( new CommandObject( CommandObject.UNLOAD_SOUND, filename ) );
        // Wake the command thread to process commands:
        commandThread.interrupt();
    }
    
/**
 * Creates a new permanant, streaming, looping priority source with zero 
 * attenuation.  
 * @param filename The name of the sound file to play at this source.
 * @return The new source's name.
 */
    public String backgroundMusic( String filename )
    {
        // return a name for the looping background music:
        return backgroundMusic( filename, true );
    }
/**
 * Creates a new permanant, streaming, priority source with zero attenuation.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should the music loop, or play only once.
 * @return The new source's name.
 */    
    public String backgroundMusic( String filename, boolean toLoop )
    {
        //generate a random name for this source:
        String sourcename = "Background_"
                            + randomNumberGenerator.nextInt()
                            + "_" + randomNumberGenerator.nextInt();
        
        backgroundMusic( sourcename, filename, toLoop );
        // return a name for the background music:
        return sourcename;
    }
/**
 * Creates a new permanant, streaming, looping priority source with zero 
 * attenuation.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 */    
    public void backgroundMusic( String sourcename, String filename )
    {
        backgroundMusic( sourcename, filename, true );
    }
/**
 * Creates a new permanant, streaming, priority source with zero attenuation.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 */    
    public void backgroundMusic( String sourcename, String filename,
                                 boolean toLoop )
    {
        newStreamingSource( true, sourcename, filename, toLoop, 0, 0, 0,
                            SoundSystemConfig.ATTENUATION_NONE, 0 );
        // Queue a command to quick stream this new source:
        ManageSources( new CommandObject( CommandObject.QUICK_PLAY, true,
                           true, toLoop, sourcename, filename, 0, 0, 0,
                           SoundSystemConfig.ATTENUATION_NONE, 0, false ) );
        CommandQueue( new CommandObject( CommandObject.PLAY, sourcename) );
        
        commandThread.interrupt();
    }
    
/**
 * Creates a new non-streaming, non-priority source at the origin.  Default 
 * values are used for attenuation.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 */    
    public void newSource( String sourcename, String filename,
                           boolean toLoop )
    {
        newSource( sourcename, filename, toLoop, 0, 0, 0,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new non-streaming source at the origin.  Default values are used 
 * for attenuation.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 */    
    public void newSource( boolean priority, String sourcename, String filename,
                           boolean toLoop )
    {
        newSource( priority, sourcename, filename, toLoop, 0, 0, 0,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new non-streaming, non-priority source.  Default values are used 
 * for attenuation.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 */    
    public void newSource( String sourcename, String filename,
                           boolean toLoop, float x, float y, float z )
    {
        newSource( sourcename, filename, toLoop, x, y, z,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new non-streaming source.  Default values are used for 
 * attenuation.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 */    
    public void newSource( boolean priority, String sourcename, String filename,
                           boolean toLoop, float x, float y, float z )
    {
        newSource( priority, sourcename, filename, toLoop, x, y, z,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new non-streaming, non-priority source at the origin.  Default 
 * value is used for either fade-distance or rolloff factor, depending on the 
 * value of parameter 'attmodel'.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 */    
    public void newSource( String sourcename, String filename, boolean toLoop,
                           int attmodel )
    {
        newSource( sourcename, filename, toLoop, 0, 0, 0, attmodel );
    }
/**
 * Creates a new non-streaming source at the origin.  Default value is used for 
 * either fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 */    
    public void newSource( boolean priority, String sourcename, String filename,
                           boolean toLoop, int attmodel )
    {
        newSource( priority, sourcename, filename, toLoop, 0, 0, 0, attmodel );
    }
/**
 * Creates a new non-streaming, non-priority source at the origin.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newSource( String sourcename, String filename, boolean toLoop,
                           int attmodel, float distORroll )
    {
        newSource( sourcename, filename, toLoop, 0, 0, 0, attmodel,
                   distORroll );
    }
/**
 * Creates a new non-streaming source at the origin.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newSource( boolean priority, String sourcename, String filename,
                           boolean toLoop, int attmodel, float distORroll )
    {
        newSource( priority, sourcename, filename, toLoop, 0, 0, 0, attmodel,
                   distORroll );
    }
/**
 * Creates a new non-streaming, non-priority source.  Default value is used for 
 * either fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 */    
    public void newSource( String sourcename, String filename, boolean toLoop,
                           float x, float y, float z, int attmodel )
    {
        newSource( false, sourcename, filename, toLoop, x, y, z, attmodel );
    }
/**
 * Creates a new non-streaming source.  Default value is used for either 
 * fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 */    
    public void newSource( boolean priority, String sourcename, String filename,
                           boolean toLoop, float x, float y, float z,
                           int attmodel )
    {
        switch( attmodel )
        {
            case SoundSystemConfig.ATTENUATION_ROLLOFF:
                newSource( priority, sourcename, filename, toLoop, x, y, z,
                           attmodel, SoundSystemConfig.getDefaultRolloff() );
                break;
            case SoundSystemConfig.ATTENUATION_LINEAR:
                newSource( priority, sourcename, filename, toLoop, x, y, z,
                           attmodel,
                           SoundSystemConfig.getDefaultFadeDistance() );
                break;
            default:
                newSource( priority, sourcename, filename, toLoop, x, y, z,
                           attmodel, 0 );
                break;
        }
    }    
/**
 * Creates a new non-streaming, non-priority source.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newSource( String sourcename, String filename, boolean toLoop,
                           float x, float y, float z, int attmodel,
                           float distOrRoll )
    {
        newSource( false, sourcename, filename, toLoop, x, y, z, attmodel,
                   distOrRoll);
    }
/**
 * Creates a new non-streaming source.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation, fade distance, and rolloff factor.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newSource( boolean priority, String sourcename, String filename,
                           boolean toLoop, float x, float y, float z,
                           int attmodel, float distOrRoll )
    {
        CommandQueue( new CommandObject( CommandObject.NEW_SOURCE, priority,
                           false, toLoop, sourcename, filename, x, y, z,
                           attmodel, distOrRoll ) );
        commandThread.interrupt();
    }
    
/**
 * Creates a new streaming non-priority source at the origin.  Default values 
 * are used for attenuation.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 */    
    public void newStreamingSource( String sourcename, String filename,
                                    boolean toLoop )
    {
        newStreamingSource( sourcename, filename, toLoop, 0, 0, 0,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new streaming source.  Default values are used for attenuation.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 */    
    public void newStreamingSource( boolean priority, String sourcename,
                                    String filename, boolean toLoop )
    {
        newStreamingSource( priority, sourcename, filename, toLoop, 0, 0, 0,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new streaming non-priority source.  Default values are used for 
 * attenuation.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 */    
    public void newStreamingSource( String sourcename, String filename,
                                    boolean toLoop, float x, float y, float z )
    {
        newStreamingSource( sourcename, filename, toLoop, x, y, z,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new streaming source.  Default values are used for attenuation.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 */    
    public void newStreamingSource( boolean priority, String sourcename,
                                    String filename, boolean toLoop, float x,
                                    float y, float z )
    {
        newStreamingSource( priority, sourcename, filename, toLoop, x, y, z,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a new streaming non-priority source at the origin.  Default value is 
 * used for either fade-distance or rolloff factor, depending on the value of 
 * parameter 'attmodel'.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 */    
    public void newStreamingSource( String sourcename, String filename,
                                    boolean toLoop, int attmodel )
    {
        newStreamingSource( sourcename, filename, toLoop, 0, 0, 0, attmodel );
    }
/**
 * Creates a new streaming source at the origin.  Default value is used for 
 * either fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.    
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 */    
    public void newStreamingSource( boolean priority, String sourcename,
                                    String filename, boolean toLoop,
                                    int attmodel )
    {
        newStreamingSource( priority, sourcename, filename, toLoop, 0, 0, 0,
                            attmodel );
    }
/**
 * Creates a new streaming non-priority source at the origin.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newStreamingSource( String sourcename, String filename,
                                    boolean toLoop, int attmodel,
                                    float distORroll )
    {
        newStreamingSource( sourcename, filename, toLoop, 0, 0, 0, attmodel,
                   distORroll );
    }
/**
 * Creates a new streaming source at the origin.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newStreamingSource( boolean priority, String sourcename,
                                    String filename, boolean toLoop,
                                    int attmodel, float distORroll )
    {
        newStreamingSource( priority, sourcename, filename, toLoop, 0, 0, 0,
                            attmodel, distORroll );
    }
/**
 * Creates a new streaming non-priority source.  Default value is used for 
 * either fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 */    
    public void newStreamingSource( String sourcename, String filename,
                                    boolean toLoop, float x, float y, float z,
                                    int attmodel )
    {
        newStreamingSource( false, sourcename, filename, toLoop, x, y, z,
                            attmodel );
    }
/**
 * Creates a new streaming source.  Default value is used for either 
 * fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.    
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 */    
    public void newStreamingSource( boolean priority, String sourcename,
                                    String filename, boolean toLoop, float x,
                                    float y, float z, int attmodel )
    {
        switch( attmodel )
        {
            case SoundSystemConfig.ATTENUATION_ROLLOFF:
                newStreamingSource( priority, sourcename, filename, toLoop,
                              x, y, z,
                              attmodel, SoundSystemConfig.getDefaultRolloff() );
                break;
            case SoundSystemConfig.ATTENUATION_LINEAR:
                newStreamingSource( priority, sourcename, filename, toLoop,
                                   x, y, z, attmodel,
                                   SoundSystemConfig.getDefaultFadeDistance() );
                break;
            default:
                newStreamingSource( priority, sourcename, filename, toLoop,
                                    x, y, z, attmodel, 0 );
                break;
        }
    }    
/**
 * Creates a new streaming non-priority source.  
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newStreamingSource( String sourcename, String filename,
                                    boolean toLoop, float x, float y, float z,
                                    int attmodel, float distOrRoll )
    {
        newStreamingSource( false, sourcename, filename, toLoop, x, y, z,
                            attmodel, distOrRoll);
    }
/**
 * Creates a new streaming source.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */    
    public void newStreamingSource( boolean priority, String sourcename,
                                    String filename, boolean toLoop, float x,
                                    float y, float z, int attmodel,
                                    float distOrRoll )
    {
        CommandQueue( new CommandObject( CommandObject.NEW_SOURCE, priority,
                           true, toLoop, sourcename, filename, x, y, z,
                           attmodel, distOrRoll ) );
        commandThread.interrupt();
    }
    
/**
 * Creates a temporary, non-priority source at the origin and plays it.  
 * Default values are used for attenuation.  After the source finishes playing, 
 * it is removed.  Returns a randomly generated name for the new source.  NOTE: 
 * to make a source created by this method permanant, call the setActive() 
 * method using the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @return The new sorce's name.
 */    
    public String quickPlay( String filename, boolean toLoop )
    {
        return quickPlay( filename, toLoop, 0, 0, 0,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary source at the origin and plays it.  Default values are 
 * used for attenuation.  After the source finishes playing, it is removed.  
 * Returns a randomly generated name for the new source.  NOTE: to make a 
 * source created by this method permanant, call the setActive() method using 
 * the return value for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @return The new sorce's name.
 */    
    public String quickPlay( boolean priority, String filename, boolean toLoop )
    {
        return quickPlay( priority, filename, toLoop, 0, 0, 0,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary non-priority source and plays it.  Default values are 
 * used for attenuation.  After the source finishes playing, it is removed.  
 * Returns a randomly generated name for the new source.  NOTE: to make a 
 * source created by this method permanant, call the setActive() method using 
 * the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @return The new sorce's name.
 */    
    public String quickPlay( String filename, boolean toLoop, float x, float y,
                             float z )
    {
        return quickPlay( filename, toLoop, x, y, z,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary source and plays it.  Default values are used for 
 * attenuation.  After the source finishes playing, it is removed.  Returns a 
 * randomly generated name for the new source.  NOTE: to make a source created 
 * by this method permanant, call the setActive() method using the return 
 * value for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @return The new sorce's name.
 */    
    public String quickPlay( boolean priority, String filename, boolean toLoop,
                             float x, float y, float z )
    {
        return quickPlay( priority, filename, toLoop, x, y, z,
                   SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary non-priority source at the origin and plays it.  Default 
 * value is used for either fade-distance or rolloff factor, depending on the 
 * value of parameter 'attmodel'.  After the source finishes playing, it is 
 * removed.  Returns a randomly generated name for the new source.  NOTE: 
 * to make a source created by this method permanant, call the setActive() 
 * method using the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickPlay( String filename, boolean toLoop, int attmodel )
    {
        return quickPlay( filename, toLoop, 0, 0, 0, attmodel );
    }
/**
 * Creates a temporary source and plays it.  Default value is used for either 
 * fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.  After the source finishes playing, it is removed.  Returns a 
 * randomly generated name for the new source.  NOTE: to make a source created 
 * by this method permanant, call the setActive() method using the return value 
 * for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickPlay( boolean priority, String filename, boolean toLoop,
                             int attmodel )
    {
        return quickPlay( priority, filename, toLoop, 0, 0, 0, attmodel );
    }
/**
 * Creates a temporary non-priority source at the origin and plays it.  After 
 * the source finishes playing, it is removed.  Returns a randomly generated 
 * name for the new source.  NOTE: to make a source created by this method 
 * permanant, call the setActive() method using the return value for 
 * sourcename.  
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickPlay( String filename, boolean toLoop, int attmodel,
                             float distORroll )
    {
        return quickPlay( filename, toLoop, 0, 0, 0, attmodel, distORroll );
    }
/**
 * Creates a temporary source at the origin and plays it.  After the source 
 * finishes playing, it is removed.  Returns a randomly generated name for the 
 * new source.  NOTE: to make a source created by this method permanant, call 
 * the setActive() method using the return value for sourcename.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickPlay( boolean priority, String filename, boolean toLoop,
                             int attmodel, float distORroll )
    {
        return quickPlay( priority, filename, toLoop, 0, 0, 0, attmodel,
                          distORroll );
    }
/**
 * Creates a temporary non-priority source and plays it.  Default value is used 
 * for either fade-distance or rolloff factor, depending on the value of 
 * parameter 'attmodel'.  After the source finishes playing, it is removed.  
 * Returns a randomly generated name for the new source.  NOTE: to make a 
 * source created by this method permanant, call the setActive() method using 
 * the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickPlay( String filename, boolean toLoop, float x, float y,
                             float z, int attmodel )
    {
        return quickPlay( false, filename, toLoop, x, y, z, attmodel );
    }
/**
 * Creates a temporary source and plays it.  Default value is used for either 
 * fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.  After the source finishes playing, it is removed.  Returns a 
 * randomly generated name for the new source.  NOTE: to make a source created 
 * by this method permanant, call the setActive() method using the return value 
 * for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickPlay( boolean priority, String filename, boolean toLoop,
                             float x, float y, float z, int attmodel )
    {
        switch( attmodel )
        {
            case SoundSystemConfig.ATTENUATION_ROLLOFF:
                return quickPlay( priority, filename, toLoop, x, y, z, attmodel,
                                  SoundSystemConfig.getDefaultRolloff() );
            case SoundSystemConfig.ATTENUATION_LINEAR:
                return quickPlay( priority, filename, toLoop, x, y, z, attmodel,
                                  SoundSystemConfig.getDefaultFadeDistance() );
            default:
                return quickPlay( priority, filename, toLoop, x, y, z, attmodel,
                                  0 );
        }
    }    
/**
 * Creates a temporary non-priority source and plays it.  After the source 
 * finishes playing, it is removed.  Returns a randomly generated name for the 
 * new source.  NOTE: to make a source created by this method permanant, call 
 * the setActive() method using the return value for sourcename.  
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickPlay( String filename, boolean toLoop, float x, float y,
                             float z, int attmodel, float distOrRoll )
    {
        return quickPlay( false, filename, toLoop, x, y, z, attmodel,
                          distOrRoll );
    }
/**
 * Creates a temporary source and plays it.  After the source finishes playing, 
 * it is removed.  Returns a randomly generated name for the new source.  NOTE: 
 * to make a source created by this method permanant, call the setActive() 
 * method using the return value for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickPlay( boolean priority, String filename, boolean toLoop,
                             float x, float y, float z, int attmodel,
                             float distOrRoll )
    {
        //generate a random name for this source:
        String sourcename = "Source_"
                            + randomNumberGenerator.nextInt()
                            + "_" + randomNumberGenerator.nextInt();
        
        // Queue a command to quick play this new source:
        ManageSources( new CommandObject( CommandObject.QUICK_PLAY, priority,
                           false, toLoop, sourcename, filename, x, y, z,
                           attmodel, distOrRoll, true ) );
        CommandQueue( new CommandObject( CommandObject.PLAY, sourcename) );
        // Wake the command thread to process commands:
        commandThread.interrupt();
        
        // return the new source name.
        return sourcename;
    }

/**
 * Creates a temporary, non-priority source at the origin and streams it.  
 * Default values are used for attenuation.  After the source finishes playing, 
 * it is removed.  Returns a randomly generated name for the new source.  NOTE: 
 * to make a source created by this method permanant, call the setActive() 
 * method using the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @return The new sorce's name.
 */    
    public String quickStream( String filename, boolean toLoop )
    {
        return quickStream( filename, toLoop, 0, 0, 0,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary source at the origin and streams it.  Default values are 
 * used for attenuation.  After the source finishes playing, it is removed.  
 * Returns a randomly generated name for the new source.  NOTE: to make a 
 * source created by this method permanant, call the setActive() method using 
 * the return value for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @return The new sorce's name.
 */    
    public String quickStream( boolean priority, String filename,
                               boolean toLoop )
    {
        return quickStream( priority, filename, toLoop, 0, 0, 0,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary non-priority source and streams it.  Default values are 
 * used for attenuation.  After the source finishes playing, it is removed.  
 * Returns a randomly generated name for the new source.  NOTE: to make a 
 * source created by this method permanant, call the setActive() method using 
 * the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @return The new sorce's name.
 */    
    public String quickStream( String filename, boolean toLoop, float x, 
                               float y, float z )
    {
        return quickStream( filename, toLoop, x, y, z,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary source and streams it.  Default values are used for 
 * attenuation.  After the source finishes playing, it is removed.  Returns a 
 * randomly generated name for the new source.  NOTE: to make a source created 
 * by this method permanant, call the setActive() method using the return 
 * value for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @return The new sorce's name.
 */    
    public String quickStream( boolean priority, String filename,
                               boolean toLoop, float x, float y, float z )
    {
        return quickStream( priority, filename, toLoop, x, y, z,
                            SoundSystemConfig.getDefaultAttenuation() );
    }
/**
 * Creates a temporary non-priority source at the origin and streams it.  
 * Default value is used for either fade-distance or rolloff factor, depending 
 * on the value of parameter 'attmodel'.  After the source finishes playing, it 
 * is removed.  Returns a randomly generated name for the new source.  NOTE: 
 * to make a source created by this method permanant, call the setActive() 
 * method using the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickStream( String filename, boolean toLoop, int attmodel )
    {
        return quickStream( filename, toLoop, 0, 0, 0, attmodel );
    }
/**
 * Creates a temporary source and streams it.  Default value is used for either 
 * fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.  After the source finishes playing, it is removed.  Returns a 
 * randomly generated name for the new source.  NOTE: to make a source created 
 * by this method permanant, call the setActive() method using the return value 
 * for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickStream( boolean priority, String filename, 
                               boolean toLoop, int attmodel )
    {
        return quickStream( priority, filename, toLoop, 0, 0, 0, attmodel );
    }
/**
 * Creates a temporary non-priority source at the origin and streams it.  After 
 * the source finishes playing, it is removed.  Returns a randomly generated 
 * name for the new source.  NOTE: to make a source created by this method 
 * permanant, call the setActive() method using the return value for 
 * sourcename.  
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickStream( String filename, boolean toLoop, int attmodel,
                               float distORroll )
    {
        return quickStream( filename, toLoop, 0, 0, 0, attmodel, distORroll );
    }
/**
 * Creates a temporary source at the origin and streams it.  After the source 
 * finishes playing, it is removed.  Returns a randomly generated name for the 
 * new source.  NOTE: to make a source created by this method permanant, call 
 * the setActive() method using the return value for sourcename.  
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickStream( boolean priority, String filename, 
                               boolean toLoop, int attmodel, float distORroll )
    {
        return quickStream( priority, filename, toLoop, 0, 0, 0, attmodel,
                            distORroll );
    }
/**
 * Creates a temporary non-priority source and streams it.  Default value is 
 * used for either fade-distance or rolloff factor, depending on the value of 
 * parameter 'attmodel'.  After the source finishes playing, it is removed.  
 * Returns a randomly generated name for the new source.  NOTE: to make a 
 * source created by this method permanant, call the setActive() method using 
 * the return value for sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickStream( String filename, boolean toLoop, float x, 
                               float y, float z, int attmodel )
    {
        return quickStream( false, filename, toLoop, x, y, z, attmodel );
    }
/**
 * Creates a temporary source and streams it.  Default value is used for either 
 * fade-distance or rolloff factor, depending on the value of parameter 
 * 'attmodel'.  After the source finishes playing, it is removed.  Returns a 
 * randomly generated name for the new source.  NOTE: to make a source created 
 * by this method permanant, call the setActive() method using the return value 
 * for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @return The new sorce's name.
 */    
    public String quickStream( boolean priority, String filename, 
                               boolean toLoop, float x, float y, float z, 
                               int attmodel )
    {
        switch( attmodel )
        {
            case SoundSystemConfig.ATTENUATION_ROLLOFF:
                return quickStream( priority, filename, toLoop, x, y, z, 
                                    attmodel, 
                                    SoundSystemConfig.getDefaultRolloff() );
            case SoundSystemConfig.ATTENUATION_LINEAR:
                return quickStream( priority, filename, toLoop, x, y, z, 
                                  attmodel,
                                  SoundSystemConfig.getDefaultFadeDistance() );
            default:
                return quickStream( priority, filename, toLoop, x, y, z, 
                                    attmodel, 0 );
        }
    }    
/**
 * Creates a temporary non-priority source and streams it.  After the source 
 * finishes playing, it is removed.  Returns a randomly generated name for the 
 * new source.  NOTE: to make a source created by this method permanant, call 
 * the setActive() method using the return value for sourcename.  
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickStream( String filename, boolean toLoop, float x, 
                               float y, float z, int attmodel, 
                               float distOrRoll )
    {
        return quickStream( false, filename, toLoop, x, y, z, attmodel,
                            distOrRoll );
    }
/**
 * Creates a temporary source and streams it.  After the source finishes 
 * playing, it is removed.  Returns a randomly generated name for the new 
 * source.  NOTE: to make a source created by this method permanant, call the 
 * setActive() method using the return value for sourcename.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param filename The name of the sound file to play at this source.
 * @param toLoop Should this source loop, or play only once.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @return The new sorce's name.
 */    
    public String quickStream( boolean priority, String filename, 
                               boolean toLoop, float x, float y, float z, 
                               int attmodel, float distOrRoll )
    {
        //generate a random name for this source:
        String sourcename = "Source_"
                            + randomNumberGenerator.nextInt()
                            + "_" + randomNumberGenerator.nextInt();
        
        // Queue a command to quick stream this new source:
        ManageSources( new CommandObject( CommandObject.QUICK_PLAY, priority,
                           true, toLoop, sourcename, filename, x, y, z,
                           attmodel, distOrRoll, true ) );
        CommandQueue( new CommandObject( CommandObject.PLAY, sourcename) );
        // Wake the command thread to process commands:
        commandThread.interrupt();
        
        // return the new source name.
        return sourcename;
    }
    
/**
 * Move a source to the specified location.  
 * @param sourcename Identifier for the source.
 * @param x destination X coordinate.
 * @param y destination Y coordinate.
 * @param z destination Z coordinate.
 */    
    public void setPosition( String sourcename, float x, float y, float z )
    {
        CommandQueue( new CommandObject( CommandObject.SET_POSITION,
                                         sourcename, x, y, z ) );
        commandThread.interrupt();
    }
/**
 * Manually sets the specified source's volume.
 * @param sourcename Source to move.
 * @param value New volume, float value ( 0.0f - 1.0f ).
 */
    public void setVolume( String sourcename, float value )
    {
        CommandQueue( new CommandObject( CommandObject.SET_VOLUME,
                                         sourcename, value ) );
        commandThread.interrupt();
    }

/**
 * Returns the current volume of the specified source, or zero if the specified 
 * source was not found.
 * @param sourcename Source to read volume from.
 * @return Float value representing the source volume (0.0f - 1.0f).
 */
    public float getVolume( String sourcename )
    {
        if( soundLibrary != null )
            return soundLibrary.getVolume( sourcename );
        else
            return 0.0f;
    }
/**
 * Set a source's priority factor.  A priority source will not be overriden when
 * too many sources are playing at once.
 * @param sourcename Identifier for the source.
 * @param pri Setting this to true makes this source a priority source.
 */    
    public void setPriority( String sourcename, boolean pri )
    {
        CommandQueue( new CommandObject( CommandObject.SET_PRIORITY,
                                         sourcename, pri ) );
        commandThread.interrupt();
    }
/**
 * Changes a source to looping or non-looping.
 * @param sourcename Identifier for the source.
 * @param lp This source should loop.
 */    
    public void setLooping( String sourcename, boolean lp )
    {
        CommandQueue( new CommandObject( CommandObject.SET_LOOPING,
                                         sourcename, lp ) );
        commandThread.interrupt();
    }
/**
 * Changes a source's attenuation model.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation.  
 * @param sourcename Identifier for the source.
 * @param model Attenuation model to use.
 */    
    public void setAttenuation( String sourcename, int model )
    {
        CommandQueue( new CommandObject( CommandObject.SET_ATTENUATION,
                                         sourcename, model ) );
        commandThread.interrupt();
    }
/**
 * Changes a source's fade distance or rolloff factor.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about fade distance and rolloff.  
 * @param sourcename Identifier for the source.
 * @param dr Either the fading distance or rolloff factor, depending on the attenuation model used.
 */    
    public void setDistOrRoll( String sourcename, float dr)
    {
        CommandQueue( new CommandObject( CommandObject.SET_DIST_OR_ROLL,
                                         sourcename, dr ) );
        commandThread.interrupt();
    }
    
/**
 * Plays the specified source.
 * @param sourcename Identifier for the source.
 */    
    public void play( String sourcename )
    {
        CommandQueue( new CommandObject( CommandObject.PLAY, sourcename) );
        commandThread.interrupt();
    }
/**
 * Pauses the specified source.
 * @param sourcename Identifier for the source.
 */    
    public void pause( String sourcename )
    {
        CommandQueue( new CommandObject( CommandObject.PAUSE, sourcename) );
        commandThread.interrupt();
    }
/**
 * Stops the specified source.
 * @param sourcename Identifier for the source.
 */    
    public void stop( String sourcename )
    {
        CommandQueue( new CommandObject( CommandObject.STOP, sourcename) );
        commandThread.interrupt();
    }
/**
 * Rewinds the specified source.
 * @param sourcename Identifier for the source.
 */    
    public void rewind( String sourcename )
    {
        CommandQueue( new CommandObject( CommandObject.REWIND, sourcename) );
        commandThread.interrupt();
    }

/**
 * Culls the specified source.  A culled source can not be played until it has 
 * been activated again.
 * @param sourcename Identifier for the source.
 */    
    public void cull( String sourcename )
    {
        ManageSources( new CommandObject( CommandObject.CULL, sourcename) );
        commandThread.interrupt();
    }
    
/**
 * Activates the specified source after it was culled, so it can be played 
 * again.  
 * @param sourcename Identifier for the source.
 */    
    public void activate( String sourcename )
    {
        ManageSources( new CommandObject( CommandObject.ACTIVATE, sourcename) );
        commandThread.interrupt();
    }
    
/**
 * Sets a flag for a source indicating whether it should be used or if it 
 * should be removed after it finishes playing.  One possible use for this 
 * method is to make temporary sources that were created with quickPlay() 
 * permanant.  Another use could be to have a source automatically removed 
 * after it finishes playing.  NOTE: Setting a source to temporary does not
 * stop it, and setting a source to permanant does not play it.  It is also 
 * important to note that a looping inactive source will not be removed as 
 * long as it keeps playing.
 * @param sourcename Identifier for the source.
 * @param temporary True = temporary, False = permanant.
 */    
    public synchronized void setTemporary( String sourcename,
                                           boolean temporary )
    {
        // Make sure there is a sound library
        if( soundLibrary == null )
        {
            errorMessage(
           "Variable 'soundLibrary' null in method 'setActive'", 0 );
            return;
        }
        // See if the source exists:
        if( !soundLibrary.getSources().containsKey( sourcename ) )
        {
            // The source may not have been created yet since commands run 
            // on a seperate thread.  Queue a new command to be processed 
            // after the source has been created.
            CommandQueue( new CommandObject( CommandObject.SET_TEMPORARY, 
                                             sourcename, temporary ) );
            
        }
        else
        {
            // The source already exists.  Since this method is synchronized, 
            // it is safe to simply set the boolean here to save time.
            soundLibrary.setTemporary( sourcename, temporary );
        }
    }
    
/**
 * Removes the specified source and clears up any memory it used.
 * @param sourcename Identifier for the source.
 */    
    public void removeSource( String sourcename )
    {
        CommandQueue( new CommandObject( CommandObject.REMOVE_SOURCE,
                                         sourcename ) );
        commandThread.interrupt();
    }
/**
 * Moves the listener relative to the current location.
 * @param x X offset.
 * @param y Y offset.
 * @param z Z offset.
 */    
    public void moveListener( float x, float y, float z )
    {
        CommandQueue( new CommandObject( CommandObject.MOVE_LISTENER,
                                         x, y, z ) );
        commandThread.interrupt();
    }
/**
 * Moves the listener to the specified location.
 * @param x Destination X coordinate.
 * @param y Destination Y coordinate.
 * @param z Destination Z coordinate.
 */    
    public void setListenerPosition( float x, float y, float z )
    {
        CommandQueue( new CommandObject( CommandObject.SET_LISTENER_POSITION,
                                         x, y, z ) );
        commandThread.interrupt();
    }
/**
 * Turns the listener counterclockwise by "angle" radians around the y-axis, 
 * relative to the current angle.
 * @param angle radian offset.
 */    
    public void turnListener( float angle )
    {
        CommandQueue( new CommandObject( CommandObject.TURN_LISTENER,
                                         angle ) );
        commandThread.interrupt();
    }
/**
 * Sets the listener's angle in radians around the y-axis.
 * @param angle radians.
 */    
    public void setListenerAngle( float angle )
    {
        CommandQueue( new CommandObject( CommandObject.SET_LISTENER_ANGLE,
                                         angle ) );
        commandThread.interrupt();
    }    
/**
 * Sets the listener's orientation.
 * @param lookX X coordinate of the (normalized) look-at vector.
 * @param lookY Y coordinate of the (normalized) look-at vector.
 * @param lookZ Z coordinate of the (normalized) look-at vector.
 * @param upX X coordinate of the (normalized) up-direction vector.
 * @param upY Y coordinate of the (normalized) up-direction vector.
 * @param upZ Z coordinate of the (normalized) up-direction vector.
 */    
    public void setListenerOrientation( float lookX, float lookY, float lookZ,
                                        float upX, float upY, float upZ )
    {
        CommandQueue( new CommandObject( CommandObject.SET_LISTENER_ORIENTATION,
                                         lookX, lookY, lookZ, upX, upY, upZ ) );
        commandThread.interrupt();
    }
    
/**
 * Sets the overall volume, affecting all sources.
 * @param value New volume, float value ( 0.0f - 1.0f ).
 */
    public void setMasterVolume( float value )
    {
        CommandQueue( new CommandObject( CommandObject.SET_MASTER_VOLUME,
                                         value ) );
        commandThread.interrupt();
    }
    
/**
 * Returns the overall volume, affecting all sources.
 * @return Float value representing the master volume (0.0f - 1.0f).
 */
    public float getMasterVolume()
    {
        return SoundSystemConfig.getMasterGain();
    }
    
/**
 * Method for obtaining information about the listener's position and 
 * orientation.  
 * @return a {@link paulscode.sound.ListenerData ListenerData} object.
 */
    public synchronized ListenerData getListenerData()
    {
        return soundLibrary.getListenerData();
    }
/**
 * Switches to the specified library, and preserves all sources.  
 * @param library Library to use.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for
 * information about chosing a sound library.
 */
    public synchronized boolean switchLibrary( int library )
                                                    throws SoundSystemException
    {
        initialized( SET, false );
        
        HashMap<String, Source> sourceMap = null;
        ListenerData listenerData = null;
        
        boolean wasMidiChannel = false;
        MidiChannel midiChannel = null;
        String midiFilename = "";
        String midiSourcename = "";
        boolean midiToLoop = true;
        
        if( soundLibrary != null )
        {
            currentLibrary( SET, -1 );
            sourceMap = copySources( soundLibrary.getSources() );
            listenerData = soundLibrary.getListenerData();
            midiChannel = soundLibrary.getMidiChannel();
            if( midiChannel != null )
            {
                wasMidiChannel = true;
                midiToLoop = midiChannel.getLooping();
                midiSourcename = midiChannel.getSourcename();
                midiFilename = midiChannel.getFilename();
            }
            
            soundLibrary.cleanup();
            soundLibrary = null;
        }
        message( "", 0 );
        message( "Switching to "
                 + SoundSystemConfig.getLibraryTitle( library ), 0 );
        message( "(" + SoundSystemConfig.getLibraryDescription( library )
                 + ")", 1 );

        try
        {
            switch( library )
            {
                case SoundSystemConfig.LIBRARY_NOSOUND:
                    soundLibrary = new Library();
                    currentLibrary( SET, SoundSystemConfig.LIBRARY_NOSOUND );
                    break;
                case SoundSystemConfig.LIBRARY_OPENAL:
                    soundLibrary = new LibraryOpenAL();
                    currentLibrary( SET, SoundSystemConfig.LIBRARY_OPENAL );
                    break;
                case SoundSystemConfig.LIBRARY_JAVASOUND:
                    soundLibrary = new LibraryJavaSound();
                    currentLibrary( SET, SoundSystemConfig.LIBRARY_JAVASOUND );
                    break;
                default:
                    errorMessage( "Library type unrecognized!!", 1 );
                    SoundSystemException sse = new SoundSystemException( 
                                        className + "did not load properly.  " +
                                        "Library type was not recognized.", 
                                        SoundSystemException.LIBRARY_TYPE );
                    lastException( SET, sse );
                    throw sse;
            }
            if( errorCheck( soundLibrary == null, "Library null after " +
                            "initialization in method 'switchLibrary'", 1 ) )
            {
                SoundSystemException sse = new SoundSystemException( 
                                       className + "did not load properly.  " +
                                       "Library was null after initialization.", 
                                       SoundSystemException.LIBRARY_NULL );
                lastException( SET, sse );
                throw sse;
            }
        }
        catch( SoundSystemException sse )
        {
            lastException( SET, sse );
            throw sse;
        }
        if( errorCheck( soundLibrary == null, "Library null after " +
                        "initialization in method 'switchLibrary'", 1 ) )
        {
            SoundSystemException sse = new SoundSystemException( 
                                   className + "did not load properly.  " +
                                   "Library was null after initialization.", 
                                   SoundSystemException.LIBRARY_NULL );
            lastException( SET, sse );
            throw sse;
        }
        try
        {
            soundLibrary.init();
        }
        catch( SoundSystemException sse )
        {
            lastException( SET, sse );
            throw sse;
        }
        soundLibrary.setListenerData( listenerData );
        if( wasMidiChannel )
        {
            if( midiChannel != null )
                midiChannel.cleanup();
            midiChannel = new MidiChannel( midiToLoop, midiSourcename,
                                           midiFilename );
            soundLibrary.setMidiChannel( midiChannel );
        }
        soundLibrary.copySources( sourceMap );
        
        message( "", 0 );
        
        lastException( SET, null );
        initialized( SET, true );
        
        return true;
    }
    
/**
 * Switches to the specified library, loosing all sources.  
 * @param library Library to use.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for
 * information about chosing a sound library.
 */
    public synchronized boolean newLibrary( int library )
                                                    throws SoundSystemException
    {
        initialized( SET, false );
        
        String headerMessage = "Initializing ";
        if( soundLibrary != null )
        {
            currentLibrary( SET, -1 );
            // we are switching libraries
            headerMessage = "Switching to ";
            soundLibrary.cleanup();
            soundLibrary = null;
        }
        message( headerMessage + SoundSystemConfig.getLibraryTitle( library ),
                 0 );
        message( "(" + SoundSystemConfig.getLibraryDescription( library )
                + ")", 1 );
        try
        {
            switch( library )
            {
                case SoundSystemConfig.LIBRARY_NOSOUND:
                    soundLibrary = new Library();
                    currentLibrary( SET, SoundSystemConfig.LIBRARY_NOSOUND );
                    break;
                case SoundSystemConfig.LIBRARY_OPENAL:
                    soundLibrary = new LibraryOpenAL();
                    currentLibrary( SET, SoundSystemConfig.LIBRARY_OPENAL );
                    break;
                case SoundSystemConfig.LIBRARY_JAVASOUND:
                    soundLibrary = new LibraryJavaSound();
                    currentLibrary( SET, SoundSystemConfig.LIBRARY_JAVASOUND );
                    break;
                default:
                    errorMessage( "Library type unrecognized!!", 1 );
                    SoundSystemException sse = new SoundSystemException( 
                                        className + "did not load properly.  " +
                                        "Library type was not recognized.", 
                                        SoundSystemException.LIBRARY_TYPE );
                    lastException( SET, sse );
                    throw sse;
            }
            if( errorCheck( soundLibrary == null, "Library null after " +
                            "initialization in method 'newLibrary'", 1 ) )
            {
                SoundSystemException sse = new SoundSystemException( 
                                       className + "did not load properly.  " +
                                       "Library was null after initialization.", 
                                       SoundSystemException.LIBRARY_NULL );
                lastException( SET, sse );
                throw sse;
            }
        }
        catch( SoundSystemException sse )
        {
            lastException( SET, sse );
            throw sse;
        }
        
        if( errorCheck( soundLibrary == null, "Library null after " +
                        "initialization in method 'newLibrary'", 1 ) )
        {
            SoundSystemException sse = new SoundSystemException( 
                                   className + "did not load properly.  " +
                                   "Library was null after initialization.", 
                                   SoundSystemException.LIBRARY_NULL );
            lastException( SET, sse );
            throw sse;
        }
        try
        {
            soundLibrary.init();
        }
        catch( SoundSystemException sse )
        {
            lastException( SET, sse );
            throw sse;
        }
        
        lastException( SET, null );
        initialized( SET, true );
        
        return true;
    }
    
/**
 * Calls the library's initialize() method.
 */
    protected void CommandInitialize()
    {
        try
        {
            if( errorCheck( soundLibrary == null, "Library null after " +
                            "initialization in method 'CommandInitialize'",
                            1 ) )
            {
                SoundSystemException sse = new SoundSystemException( 
                                       className + "did not load properly.  " +
                                       "Library was null after initialization.", 
                                       SoundSystemException.LIBRARY_NULL );
                lastException( SET, sse );
                throw sse;
            }
            soundLibrary.init();
        }
        catch( SoundSystemException sse )
        {
            lastException( SET, sse );
            initialized( SET, true );
        }
    }
    
/**
 * Loads a sound file into memory.
 * @param filename Sound file to load.
 */
    protected void CommandLoadSound( String filename )
    {
        if( soundLibrary != null )
            soundLibrary.loadSound( filename );
        else
            errorMessage(
               "Variable 'soundLibrary' null in method 'CommandLoadSound'", 0 );
    }
    
/**
 * Removes a previously loaded sound file from memory.
 * @param filename Sound file to load.
 */
    protected void CommandUnloadSound( String filename )
    {
        if( soundLibrary != null )
            soundLibrary.unloadSound( filename );
        else
            errorMessage(
               "Variable 'soundLibrary' null in method 'CommandLoadSound'", 0 );
    }
    
/**
 * Loads a sound file into memory.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param toStream Whether or not to stream the source.
 * @param toLoop Whether or not to loop the source.
 * @param sourcename A unique identifier for the source.
 * @param filename The name of the sound file to play at this source.
 * @param posX X position for this source.
 * @param posY Y position for this source.
 * @param posZ Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */
    protected void CommandNewSource( boolean priority, boolean toStream,
                                     boolean toLoop, String sourcename,
                                     String filename, float posX, float posY,
                                     float posZ, int attModel,
                                     float distORroll )
    {
        if( soundLibrary != null )
        {
            if( filename.matches( SoundSystemConfig.EXTENSION_MIDI ) )
            {
                soundLibrary.loadMidi( toLoop, sourcename, filename );
            }
            else
            {
                soundLibrary.newSource( priority, toStream, toLoop, sourcename,
                                        filename, posX, posY, posZ, attModel,
                                        distORroll );
            }
        }
        else
            errorMessage(
               "Variable 'soundLibrary' null in method 'CommandNewSource'", 0 );
    }
/**
 * Creates a temporary source and either plays or streams it.  After the source 
 * finishes playing, it is removed.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param toStream Whether or not to stream the source.
 * @param toLoop Whether or not to loop the source.
 * @param sourcename A unique identifier for the source.
 * @param filename The name of the sound file to play at this source.
 * @param posX X position for this source.
 * @param posY Y position for this source.
 * @param posZ Z position for this source.
 * @param attmodel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 * @param tmp Whether or not the source should be removed after it finishes playing.
 */
    protected void CommandQuickPlay( boolean priority, boolean toStream,
                                     boolean toLoop, String sourcename,
                                     String filename, float posX, float posY,
                                     float posZ, int attModel, float distORroll, 
                                     boolean tmp )
    {
        if( soundLibrary != null )
        {
            if( filename.matches( SoundSystemConfig.EXTENSION_MIDI ) )
            {
                soundLibrary.loadMidi( toLoop, sourcename, filename );
            }
            else
            {
                soundLibrary.quickPlay( priority, toStream, toLoop, sourcename,
                                        filename, posX, posY, posZ, attModel,
                                        distORroll, tmp );
            }
        }
        else
            errorMessage(
               "Variable 'soundLibrary' null in method 'CommandQuickPlay'", 0 );
    }
/**
 * Moves a source to the specified coordinates.
 * @param sourcename Source to move.
 * @param x Destination X coordinate.
 * @param y Destination Y coordinate.
 * @param z Destination Z coordinate.
 */
    protected void CommandSetPosition( String sourcename, float x, float y,
                                       float z)
    {
        if( soundLibrary != null )
            soundLibrary.setPosition( sourcename, x, y, z );
        else
            errorMessage(
              "Variable 'soundLibrary' null in method 'CommandMoveSource'", 0 );
    }
/**
 * Manually sets the specified source's volume.
 * @param sourcename Source to move.
 * @param value New volume, float value ( 0.0f - 1.0f ).
 */
    protected void CommandSetVolume( String sourcename, float value )
    {
        if( soundLibrary != null )
            soundLibrary.setVolume( sourcename, value );
        else
            errorMessage(
              "Variable 'soundLibrary' null in method 'CommandSetVolume'", 0 );
    }
/**
 * Set a source's priority factor.  A priority source will not be overriden when
 * too many sources are playing at once.
 * @param sourcename Identifier for the source.
 * @param pri Setting this to true makes this source a priority source.
 */    
    protected void CommandSetPriority( String sourcename, boolean pri )
    {
        if( soundLibrary != null )
            soundLibrary.setPriority( sourcename, pri );
        else
            errorMessage(
             "Variable 'soundLibrary' null in method 'CommandSetPriority'", 0 );
    }
/**
 * Changes a source to looping or non-looping.
 * @param sourcename Identifier for the source.
 * @param lp This source should loop.
 */    
    protected void CommandSetLooping( String sourcename, boolean lp )
    {
        if( soundLibrary != null )
            soundLibrary.setLooping( sourcename, lp );
        else
            errorMessage(
              "Variable 'soundLibrary' null in method 'CommandSetLooping'", 0 );
    }
/**
 * Changes a source's attenuation model.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about Attenuation.  
 * @param sourcename Identifier for the source.
 * @param model Attenuation model to use.
 */    
    protected void CommandSetAttenuation( String sourcename, int model )
    {
        if( soundLibrary != null )
            soundLibrary.setAttenuation( sourcename, model );
        else
            errorMessage(
               "Variable 'soundLibrary' null in method 'CommandSetAttenuation'",
               0 );
    }
/**
 * Changes a source's fade distance or rolloff factor.
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more
 * information about fade distance and rolloff.  
 * @param sourcename Identifier for the source.
 * @param dr Either the fading distance or rolloff factor, depending on the attenuation model used.
 */    
    protected void CommandSetDistOrRoll( String sourcename, float dr )            
    {
        if( soundLibrary != null )
            soundLibrary.setDistOrRoll( sourcename, dr );
        else
            errorMessage(
                "Variable 'soundLibrary' null in method 'CommandSetDistOrRoll'",
                0 );
    }
/**
 * Plays the specified source.
 * @param sourcename Identifier for the source.
 */    
    protected void CommandPlay( String sourcename )
    {
        if( soundLibrary != null )
            soundLibrary.play( sourcename );
        else
            errorMessage(
                    "Variable 'soundLibrary' null in method 'CommandPlay'", 0 );
    }
/**
 * Pauses the specified source.
 * @param sourcename Identifier for the source.
 */    
    protected void CommandPause( String sourcename )
    {
        if( soundLibrary != null )
            soundLibrary.pause( sourcename );
        else
            errorMessage(
                   "Variable 'soundLibrary' null in method 'CommandPause'", 0 );
    }
/**
 * Stops the specified source.
 * @param sourcename Identifier for the source.
 */    
    protected void CommandStop( String sourcename )
    {
        if( soundLibrary != null )
            soundLibrary.stop( sourcename );
        else
            errorMessage(
                    "Variable 'soundLibrary' null in method 'CommandStop'", 0 );
    }
/**
 * Rewinds the specified source.
 * @param sourcename Identifier for the source.
 */    
    protected void CommandRewind( String sourcename )
    {
        if( soundLibrary != null )
            soundLibrary.rewind( sourcename );
        else
            errorMessage(
            "Variable 'soundLibrary' null in method 'CommandRewind'", 0 );
    }
/**
 * Sets a flag for a source indicating whether it should be used or if it 
 * should be removed after it finishes playing.  One possible use for this 
 * method is to make temporary sources that were created with quickPlay() 
 * permanant.  Another use could be to have a source automatically removed 
 * after it finishes playing.  NOTE: Setting a source to inactive does not stop 
 * it, and setting a source to active does not play it.  It is also important 
 * to note that a looping inactive source will not be removed as long as 
 * it keeps playing.
 * @param sourcename Identifier for the source.
 * @param temporary True or False.
 */
    protected void CommandSetTemporary( String sourcename, boolean temporary )
    {
        if( soundLibrary != null )
            soundLibrary.setTemporary( sourcename, temporary );        
        else
            errorMessage(
               "Variable 'soundLibrary' null in method 'CommandSetActive'", 0 );                    
    }
/**
 * Removes the specified source and clears up any memory it used.
 * @param sourcename Identifier for the source.
 */    
    protected void CommandRemoveSource( String sourcename )
    {
        if( soundLibrary != null )
            soundLibrary.removeSource( sourcename );
        else
            errorMessage(
            "Variable 'soundLibrary' null in method 'CommandRemoveSource'", 0 );                    
    }
/**
 * Moves the listener relative to the current location.
 * @param x X offset.
 * @param y Y offset.
 * @param z Z offset.
 */    
    protected void CommandMoveListener( float x, float y, float z )
    {
        if( soundLibrary != null )
            soundLibrary.moveListener( x, y, z );
        else
            errorMessage(
            "Variable 'soundLibrary' null in method 'CommandMoveListener'", 0 );
    }
 /**
 * Moves the listener to the specified location.
 * @param x Destination X coordinate.
 * @param y Destination Y coordinate.
 * @param z Destination Z coordinate.
 */    
   protected void CommandSetListenerPosition( float x, float y, float z )
    {
        if( soundLibrary != null )
            soundLibrary.setListenerPosition( x, y, z );
        else
            errorMessage(
          "Variable 'soundLibrary' null in method 'CommandSetListenerPosition'",
          0 );
    }
/**
 * Turns the listener counterclockwise by "angle" radians around the y-axis, 
 * relative to the current angle.
 * @param angle radian offset.
 */    
    protected void CommandTurnListener( float angle )
    {
        if( soundLibrary != null )
            soundLibrary.turnListener( angle );
        else
            errorMessage(
                 "Variable 'soundLibrary' null in method 'CommandTurnListener'",
                 0 );                    
    }
/**
 * Sets the listener's angle in radians around the y-axis.
 * @param angle radians.
 */    
    protected void CommandSetListenerAngle( float angle )
    {
        if( soundLibrary != null )
            soundLibrary.setListenerAngle( angle );
        else
            errorMessage(
             "Variable 'soundLibrary' null in method 'CommandSetListenerAngle'",
             0 );                    
    }
/**
 * Sets the listener's orientation.
 * @param lookX X coordinate of the (normalized) look-at vector.
 * @param lookY Y coordinate of the (normalized) look-at vector.
 * @param lookZ Z coordinate of the (normalized) look-at vector.
 * @param upX X coordinate of the (normalized) look-at vector.
 * @param upY Y coordinate of the (normalized) look-at vector.
 * @param upZ Z coordinate of the (normalized) look-at vector.
 */    
    protected void CommandSetListenerOrientation( float lookX, float lookY,
                                                float lookZ, float upX,
                                                float upY, float upZ )
    {
        if( soundLibrary != null )
            soundLibrary.setListenerOrientation( lookX, lookY, lookZ, upX, upY,
                                                 upZ );
        else
            errorMessage(
       "Variable 'soundLibrary' null in method 'CommandSetListenerOrientation'",
       0 );                    
    }
    
/**
 * Culls the specified source.  A culled source can not be played until it has 
 * been activated again.
 * @param sourcename Identifier for the source.
 */    
    protected void CommandCull( String sourcename )
    {
        if( soundLibrary != null )
            soundLibrary.cull( sourcename );
        else
            errorMessage(
                    "Variable 'soundLibrary' null in method 'CommandCull'", 0 );
    }
    
/**
 * Activates a previously culled source, so it can be played again.  
 * @param sourcename Identifier for the source.
 */    
    protected void CommandActivate( String sourcename )
    {
        if( soundLibrary != null )
            soundLibrary.activate( sourcename );
        else
            errorMessage(
                     "Variable 'soundLibrary' null in method 'CommandActivate'",
                     0 );
    }
    
/**
 * Sets the overall volume, affecting all sources.
 * @param value New volume, float value ( 0.0f - 1.0f ).
 */
    protected void CommandSetMasterVolume( float value )
    {
        if( soundLibrary != null )
            soundLibrary.setMasterVolume( value );
        else
            errorMessage(
              "Variable 'soundLibrary' null in method 'CommandSetMasterVolume'",
              0 );
    }
    
/**
 * Handles commands which must be executed before all other commands, such as 
 * cull and activate commands, to ensure that the correct sources will be 
 * played.  If newCommand is null, all commands are dequeued and executed.  
 * This is automatically used by the sound system, so it is not likely that a 
 * user would ever need to use this method.  
 * See {@link paulscode.sound.CommandQueue CommandQueue} for more information 
 * about commands.  
 * @param newCommand Command to queue, or null to execute commands.  
 * @return True if more commands exist, false if queue is empty.
 */
    public synchronized boolean ManageSources( CommandObject newCommand )
    {
        if( newCommand == null )
        {
            boolean activations = false;
            // execute a queued command
            CommandObject commandObject;
            while( sourceManagementList != null
                   && sourceManagementList.size() > 0 )
            {
                commandObject = sourceManagementList.remove( 0 );
                if( commandObject != null )
                {
                    switch( commandObject.Command )
                    {
                        case CommandObject.CULL:
                            CommandCull( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.ACTIVATE:
                            activations = true;
                            CommandActivate( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.QUICK_PLAY:
                            CommandQuickPlay( commandObject.boolArgs[0],
                                    commandObject.boolArgs[1],
                                    commandObject.boolArgs[2],
                                    commandObject.stringArgs[0],
                                    commandObject.stringArgs[1],
                                    commandObject.floatArgs[0],
                                    commandObject.floatArgs[1],
                                    commandObject.floatArgs[2],
                                    commandObject.intArgs[0],
                                    commandObject.floatArgs[3],
                                    commandObject.boolArgs[3] );
                            break;
                        default:
                            break;
                    }
                }
            }
            if( activations )
                soundLibrary.replaySources();
            
            return( sourceManagementList != null
                    && sourceManagementList.size() > 0 );
        }
        else
        {
            // make sure the commandQueue exists:
            if( sourceManagementList == null )
                return false;
            
            // queue a new command
            sourceManagementList.add( newCommand );
            return true;
        }
    }
    
/**
 * Queues a command.  
 * If newCommand is null, all commands are dequeued and executed.  
 * This is automatically used by the sound system, so it is not 
 * likely that a user would ever need to use this method.  
 * See {@link paulscode.sound.CommandQueue CommandQueue} for more information 
 * about commands.  
 * @param newCommand Command to queue, or null to execute commands.  
 * @return True if more commands exist, false if queue is empty.
 */
    public synchronized boolean CommandQueue( CommandObject newCommand )
    {
        if( newCommand == null )
        {
            // execute a queued command
            CommandObject commandObject;
            while( commandQueue != null && commandQueue.size() > 0 )
            {
                commandObject = commandQueue.remove( 0 );
                if( commandObject != null )
                {
                    switch( commandObject.Command )
                    {
                        case CommandObject.INITIALIZE:
                            CommandInitialize();
                            break;
                        case CommandObject.LOAD_SOUND:
                            CommandLoadSound( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.UNLOAD_SOUND:
                            CommandUnloadSound( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.NEW_SOURCE:
                            CommandNewSource( commandObject.boolArgs[0],
                                    commandObject.boolArgs[1],
                                    commandObject.boolArgs[2],
                                    commandObject.stringArgs[0],
                                    commandObject.stringArgs[1],
                                    commandObject.floatArgs[0],
                                    commandObject.floatArgs[1],
                                    commandObject.floatArgs[2],
                                    commandObject.intArgs[0],
                                    commandObject.floatArgs[3] );
                            break;
                        case CommandObject.SET_POSITION:
                            CommandSetPosition( commandObject.stringArgs[0],
                                    commandObject.floatArgs[0],
                                    commandObject.floatArgs[1],
                                    commandObject.floatArgs[2] );
                            break;
                        case CommandObject.SET_VOLUME:
                            CommandSetVolume( commandObject.stringArgs[0],
                                    commandObject.floatArgs[0] );
                            break;
                        case CommandObject.SET_PRIORITY:
                            CommandSetPriority( commandObject.stringArgs[0],
                                                commandObject.boolArgs[0] );
                            break;
                        case CommandObject.SET_LOOPING:
                            CommandSetLooping( commandObject.stringArgs[0],
                                               commandObject.boolArgs[0] );
                            break;
                        case CommandObject.SET_ATTENUATION:
                            CommandSetAttenuation( commandObject.stringArgs[0],
                                                   commandObject.intArgs[0] );
                            break;
                        case CommandObject.SET_DIST_OR_ROLL:
                            CommandSetDistOrRoll( commandObject.stringArgs[0],
                                                  commandObject.floatArgs[0] );
                            break;
                        case CommandObject.PLAY:
                            CommandPlay( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.PAUSE:
                            CommandPause( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.STOP:
                            CommandStop( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.REWIND:
                            CommandRewind( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.SET_TEMPORARY:
                            CommandSetTemporary( commandObject.stringArgs[0],
                                              commandObject.boolArgs[0] );
                            break;
                        case CommandObject.REMOVE_SOURCE:
                            CommandRemoveSource( commandObject.stringArgs[0] );
                            break;
                        case CommandObject.MOVE_LISTENER:
                            CommandMoveListener( commandObject.floatArgs[0],
                                    commandObject.floatArgs[1],
                                    commandObject.floatArgs[2]);
                            break;
                        case CommandObject.SET_LISTENER_POSITION:
                            CommandSetListenerPosition(
                                    commandObject.floatArgs[0],
                                    commandObject.floatArgs[1],
                                    commandObject.floatArgs[2]);
                            break;
                        case CommandObject.TURN_LISTENER:
                            CommandTurnListener( commandObject.floatArgs[0] );
                            break;
                        case CommandObject.SET_LISTENER_ANGLE:
                            CommandSetListenerAngle(
                                    commandObject.floatArgs[0]);
                            break;
                        case CommandObject.SET_LISTENER_ORIENTATION:
                            CommandSetListenerOrientation(
                                    commandObject.floatArgs[0],
                                    commandObject.floatArgs[1],
                                    commandObject.floatArgs[2],
                                    commandObject.floatArgs[3],
                                    commandObject.floatArgs[4],
                                    commandObject.floatArgs[5]);
                            break;
                        case CommandObject.SET_MASTER_VOLUME:
                            CommandSetMasterVolume(
                                                   commandObject.floatArgs[0] );
                            break;
                        default:
                            break;
                    }
                }
            }
            return( commandQueue != null && commandQueue.size() > 0 );
        }
        else
        {
            // make sure the commandQueue exists:
            if( commandQueue == null )
                return false;
            
            // queue a new command
            commandQueue.add( newCommand );
            return true;
        }
    }
    
/**
 * Searches for and removes any temporary sources that have finished 
 * playing.  This method is used internally by SoundSystem, and it is 
 * unlikely that the user will ever need to use it.
 */    
    public synchronized void removeTemporarySources()
    {
        if( soundLibrary != null )
            soundLibrary.removeTemporarySources();
    }
    
/**
 * Returns true if the specified source is playing.
 * @param sourcename Unique identifier of the source to check.
 * @return True or false.
 */    
    public synchronized boolean playing( String sourcename )
    {
        if( soundLibrary == null )
            return false;

        Source src = soundLibrary.getSources().get( sourcename );

        if( src == null )
            return false;

        return src.playing();
    }

/**
 * Returns true if anything is currently playing.
 * @return True or false.
 */    
    public synchronized boolean playing()
    {
        if( soundLibrary == null )
            return false;

        HashMap<String, Source> sourceMap = soundLibrary.getSources();
        if( sourceMap == null )
            return false;

        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source source;

        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = sourceMap.get( sourcename );
            if( source != null )
                if( source.playing() )
                    return true;
        }

        return false;
    }

/**
 * Copies and returns the peripheral information from a map of sources.  This 
 * method is used internally by SoundSystem, and it is unlikely that the user 
 * will ever need to use it.
 * @param sourceMap Sources to copy.
 * @return New map of sources.
 */    
    private HashMap<String, Source> copySources( HashMap<String,
                                                 Source> sourceMap )
    {
        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source source;
        
        // New map of generic source data:
        HashMap<String, Source> returnMap = new HashMap<String, Source>();
        
        
        // loop through and store information from all the sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = sourceMap.get( sourcename );
            if( source != null )
                returnMap.put( sourcename, new Source( source, null ) );
        }
        return returnMap;
    }
    
/**
 * Empties a map of source data, shuts the sources down, and removes all the 
 * sources' references to instantiated objects.  This method is used internally 
 * by SoundSystem, and it is unlikely that the user will ever need to use it.
 * @param sourceMap Source map to empty.
 */
    private void clearSourceMap( HashMap<String, Source> sourceMap )
    {
        if( sourceMap == null )
            return;
        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source source;
        
        // loop through and cleanup all the sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = sourceMap.get( sourcename );
            if( source != null )
                source.cleanup();
        }
        
        sourceMap.clear();
    }
    
/**
 * Checks if the specified library type is compatible.
 * @param type Global identifier for Libary type.
 * @return True or false.
 */
    public static boolean libraryCompatible( int type )
    {
        // create the message logger:
        SoundSystemLogger logger = SoundSystemConfig.getLogger();
        // if the user didn't create one, then do it now:
        if( logger == null )
        {
            logger = new SoundSystemLogger();
            SoundSystemConfig.setLogger( logger );
        }
        logger.message( "", 0 );
        logger.message( "Checking if " +
                        SoundSystemConfig.getLibraryTitle(type) +
                        " is compatible...", 0 );
        
        boolean comp = SoundSystemConfig.libraryCompatible( type );
        
        if( comp )
            logger.message( "...yes", 1 );
        else
            logger.message( "...no", 1 );
            
        return comp;
    }
    
/**
 * Returns the currently loaded library, or -1 if none.
 * @return Global library identifier
 */
    public static int currentLibrary()
    {
        return( currentLibrary( GET, -1 ) );
    }
    
/**
 * Returns false if a sound library is busy initializing.
 * @return True or false.
 */
    public static boolean initialized()
    {
        return( initialized( GET, XXX ) );
    }
    
/**
 * Returns the last SoundSystemException thrown, or null if none.
 * @return The last exception.
 */
    public static SoundSystemException getLastException()
    {
        return( lastException( GET, null ) );
    }
    
/**
 * Sets or returns the value of boolean 'initialized'.
 * @param action Action to perform (GET or SET).
 * @param value New value if action is SET, otherwise XXX.
 * @return value of boolean 'initialized'.
 */
    private static synchronized boolean initialized( boolean action,
                                                     boolean value )
    {
        if( action == SET )
            initialized = value;
        return initialized;
    }
    
/**
 * Sets or returns the value of boolean 'initialized'.
 * @param action Action to perform (GET or SET).
 * @param value New value if action is SET, otherwise XXX.
 * @return value of boolean 'initialized'.
 */
    private static synchronized int currentLibrary( boolean action,
                                                    int value )
    {
        if( action == SET )
            currentLibrary = value;
        return currentLibrary;
    }
    
/**
 * Sets or returns the error code for the last error that occurred.  If no 
 * errors have occurred, returns SoundSystem.ERROR_NONE
 * @param action Action to perform (GET or SET).
 * @param e New exception if action is SET, otherwise XXX.
 * @return Last SoundSystemException thrown.
 */
    private static synchronized SoundSystemException lastException(
                                        boolean action, SoundSystemException e )
    {
        if( action == SET )
            lastException = e;
        return lastException;
    }
    
/**
 * Sleeps for the specified number of milliseconds.
 */
    protected static void snooze( long milliseconds )
    {
        try
        {
            Thread.sleep( milliseconds );
        }
        catch( InterruptedException e ){}
    }    

/**
 * Prints a message.
 * @param message Message to print.
 * @param indent Number of tabs to indent the message.
 */
    protected void message( String message, int indent )
    {
        logger.message( message, indent );
    }
    
/**
 * Prints an important message.
 * @param message Message to print.
 * @param indent Number of tabs to indent the message.
 */
    protected void importantMessage( String message, int indent )
    {
        logger.importantMessage( message, indent );
    }
    
/**
 * Prints the specified message if error is true.
 * @param error True or False.
 * @param message Message to print if error is true.
 * @param indent Number of tabs to indent the message.
 * @return True if error is true.
 */
    protected boolean errorCheck( boolean error, String message, int indent )
    {
        return logger.errorCheck( error, className, message, indent );
    }
    
/**
 * Prints an error message.
 * @param message Message to print.
 * @param indent Number of tabs to indent the message.
 */
    protected void errorMessage( String message, int indent )
    {
        logger.errorMessage( className, message, indent );
    }
}
