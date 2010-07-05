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
 * The SoundSystemConfig class is used to access global sound system settings.  
 * All members of this class are static.  Use SoundSystemConfig to change 
 * global SoundSystem settings.
 * 
 * Author: Paul Lamb
 */
public class SoundSystemConfig
{
//  GLOBAL IDENTIFIERS
    
/**
 * The "no sound" library (mute).
 */
    public static final int LIBRARY_NOSOUND      = 0;
/**
 * The lwjgl binding of OpenAL.  For more information, see 
 * http://www.lwjgl.org
 */
    public static final int LIBRARY_OPENAL       = 1;
/**
 * The Java Sound API.  For more information, see 
 * http://java.sun.com/products/java-media/sound/
 */
    public static final int LIBRARY_JAVASOUND    = 2;
    
/**
 * A normal (non-streaming) source.  Also used to define a Channel type as 
 * normal.
 */
    public static final int TYPE_NORMAL          = 0;
/**
 * A streaming source.  Also used to define a Channel type as streaming.
 */
    public static final int TYPE_STREAMING       = 1;
    
/**
 * Global identifier for no attenuation.  Attenuation is how a source's volume 
 * fades with distance.  When there is no attenuation, a source's volume 
 * remains constaint regardles of distance.
 */
    public static final int ATTENUATION_NONE     = 0;  // no attenuation
/**
 * Global identifier for rolloff attenuation.  Rolloff attenuation is a 
 * realistic attenuation model, which uses a rolloff factor to determine how 
 * quickly a source fades with distance.  A smaller rolloff factor will fade at 
 * a further distance, and a rolloff factor of 0 will never fade.  NOTE: In 
 * OpenAL, rolloff attenuation only works for monotone sounds.
 */
    public static final int ATTENUATION_ROLLOFF  = 1;  // logrithmic attenuation
/**
 * Global identifier for linear attenuation.  Linear attenuation is less
 * realistic than rolloff attenuation, but it allows the user to specify a 
 * maximum "fade distance" where a source's volume becomes zero.
 */
    public static final int ATTENUATION_LINEAR   = 2;  // linear attenuation
    
/**
 * Global identifier for a WAV format sound file.
 */
    public static final int FORMAT_WAV = 0;
/**
 * Global identifier for an OGG format sound file.
 */
    public static final int FORMAT_OGG = 1;
    
/**
 * A Regular expression for determining if a file's extension is WAV.
 */
    public static final String EXTENSION_WAV = ".*[wW][aA][vV]$";
/**
 * A Regular expression for determining if a file's extension is OGG.
 */
    public static final String EXTENSION_OGG = ".*[oO][gG][gG]$";
/**
 * A Regular expression for determining if a file's extension is MIDI.
 */
    public static final String EXTENSION_MIDI = ".*[mM][iI][dD][iI]?$";
    
/**
 * A Regular expression for determining if a path is an online URL.
 */
    public static final String PREFIX_URL = "^[hH][tT][tT][pP]://.*";
    
//  END GLOBAL IDENTIFIERS
 
    
//  PRIVATE STATIC VARIABLES

/**
 * Handle to the message logger.  The default logger can be changed by 
 * overridding the {@link paulscode.sound.SoundSystemLogger SoundSystemLogger} 
 * class and calling the setLogger() method (must be done BEFORE instantiating 
 * the SoundSystem class!)
 */
    private static SoundSystemLogger logger = null;
/**
 * Default library to use if one is not specified.
 */
    private static int defaultLibrary = LIBRARY_OPENAL;
    
/**
 * Array of library types in their order of priority.
 */
    private static int[] libraryPriorities;
    
/**
 * Maximum number of normal (non-streaming) channels that can be created.
 * NOTE: JavaSound may require the total number of channels (non-streaming + 
 * streaming) to be 32.
 */
    private static int numberNormalChannels = 28;
/**
 * Maximum number of streaming channels that can be created.
 * NOTE: JavaSound may require the total number of channels (non-streaming + 
 * streaming) to be 32.
 */
    private static int numberStreamingChannels = 4;
/**
 * Overall volume, affecting all sources.  Float value (0.0f - 1.0f).
 */
    private static float masterGain = 1.0f;
/**
 * Attenuation model to use if not specified.  Attenuation is how a source's  
 * volume fades with distance.
 */
    private static int defaultAttenuationModel = ATTENUATION_ROLLOFF;
/**
 * Default value to use for the rolloff factor if not specified.
 */
    private static float defaultRolloffFactor = 0.03f;
/**
 * Default value to use for fade distance if not specified.
 */
    private static float defaultFadeDistance = 1000.0f;
/**
 * Package where the sound files are located (must be followed by '/').
 */
    private static String soundFilesPackage = "Sounds/";
    
/**
 * Package path or URL to the default sound bank file (Required for playing 
 * midi within an applet).
 */
    private static String defaultMidiSoundBank =
                                       "paulscode/sound/resources/soundbank.gm";
/**
 * Number of bytes to load at a time when streaming.
 */
    private static int streamingBufferSize = 131072;
/**
 * Number of buffers used for each streaming sorce.
 */
    private static int numberStreamingBuffers = 2;    
/**
 * The maximum number of bytes to read in for (non-streaming) files.  
 * Increase this value if non-streaming sounds are getting cut off.  
 * Decrease this value if large sound files are causing lag during load time.  
 */
    private static int maxFileSize = 268435456;
/**
 * Size of each chunk to read at a time for loading (non-streaming) files.
 * Increase if loading sound files is causing significant lag.
 */
    private static int fileChunkSize = 1048576;
    
//  END PRIVATE STATIC VARIABLES
    
// THESE TWO METHODS PROVIDE INFORMATION ABOUT THE INDIVIDUAL SOUND LIBRARIES
    
/**
 * Returns an int array containing all sound-library types, in their 
 * order of priority.
 * @return Array of library types.
*/
    public static int[] getLibraryPriorities()
    {
        if( libraryPriorities == null )
        {
            libraryPriorities = new int[3];
            libraryPriorities[0] = LIBRARY_OPENAL;
            libraryPriorities[1] = LIBRARY_JAVASOUND;
            libraryPriorities[2] = LIBRARY_NOSOUND;
        }
        return libraryPriorities;
    }
    
/**
 * Sets the array of all sound-library types, in their order of priority.
 * @param libraries Int array.
*/
    public static void setLibraryPriorities( int[] libraries )
    {
        libraryPriorities = libraries;
    }
    
/**
 * Returns the title of the specified library.
 * @param library A global library identifier.
 * @return A short title.
*/
    public static String getLibraryTitle( int library )
    {
        switch( library )
        {
            case LIBRARY_NOSOUND:
                return "No Sound";
            case LIBRARY_OPENAL:
                return "OpenAL";
            case LIBRARY_JAVASOUND:
                return "JavaSound";
            default:
                return "Unrecognized!";
        }
    }
/**
 * Returns a description of the specified library.
 * @param library A global library identifier.
 * @return A longer description.
*/
    public static String getLibraryDescription( int library )
    {
        switch( library )
        {
            case LIBRARY_NOSOUND:
                return "Silent mode.";
            case LIBRARY_OPENAL:
                return
                ( "The lwjgl binding of OpenAL.  For more information, see " +
                  "http://www.lwjgl.org" );
            case LIBRARY_JAVASOUND:
                return
                ( "The Java Sound API.  For more information, see " +
                  "http://java.sun.com/products/java-media/sound/" );
            default:
                return "Library id " + library + "invalid!";
        }
    }
    
/**
 * Returns the class of the specified library.
 * @param library A global library identifier.
 * @return The library's class.
*/
    public static Class getLibraryClass( int library )
    {
        switch( library )
        {
            case LIBRARY_OPENAL:
                return LibraryOpenAL.class;
            case LIBRARY_JAVASOUND:
                return LibraryJavaSound.class;
            default:
                return Library.class;
        }
    }
    
/**
 * Checks if the specified library type is compatible.
 * @param library A global library identifier.
 * @return True or false.
*/
    public static boolean libraryCompatible( int library )
    {
        switch( library )
        {
            case LIBRARY_OPENAL:
                return LibraryOpenAL.libraryCompatible();
            case LIBRARY_JAVASOUND:
                return LibraryJavaSound.libraryCompatible();
            default:
                return Library.libraryCompatible();
        }
    }
    
// END LIBRARY INFORMATION

// Use the following methods to interface the private variables above:
    
// STATIC NONSYNCHRONIZED INTERFACE METHODS
/**
 * Changes the message logger to use for handling status messages, warnings, 
 * and error messages.  This method should only be called BEFORE instantiating 
 * the SoundSystem class!  If this method is called after the SoundSystem has 
 * been created, there will be handles floating around to two different 
 * loggers, and the results will be undesirable.  This method can be used to 
 * change how messages are handled.  First, the 
 * {@link paulscode.sound.SoundSystemLogger SoundSystemLogger} class should be 
 * extended and methods overriden to change how messages are handled.  Then, 
 * the overridden class should be instantiated, and a call made to 
 * SoundSystemConfig.setLogger() before creating the SoundSystem object.  
 * If an alternate logger is not set by the user before the SoundSystem is 
 * instantiated, then an instance of the base SoundSystemLogger class will be 
 * used by default.
 * @param l Handle to a message logger.
 */
    public static void setLogger( SoundSystemLogger l )
    {
        logger = l;
    }
/**
 * Returns a handle to the message logger.
 * @return The current message logger.
 */
    public static SoundSystemLogger getLogger()
    {
        return logger;
    }
    
//  STATIC SYNCHRONIZED INTERFACE METHODS
    
/**
 * Sets the default library to use when one is not specified.
 * @param library A global library identifier.
 */
    public static synchronized void setDefaultLibrary( int library )
    {
        defaultLibrary = library;
    }
    
/**
 * Returns the default library used when one is not specified.
 * @return A global library identifier
 */
    public static synchronized int getDefaultLibrary()
    {
        return defaultLibrary;
    }
    
/**
 * Sets the maximum number of normal (non-streaming) channels that can be 
 * created.  Streaming channels are created first, so the higher the maximum 
 * number of streaming channels is set, the fewer non-streaming channels will 
 * be available.  If unable to create the number of channels specified, 
 * SoundSystem will create as many as possible.
 * NOTE: JavaSound may require the total number of channels (non-streaming + 
 * streaming) to be 32.
 * @param number How many audio channels.
 */
    public static synchronized void setNumberNormalChannels( int number )
    {
        numberNormalChannels = number;
    }
    
/**
 * Returns the maximum number of normal (non-streaming) channels that can be 
 * created.
 * @return Maximum non-streaming channels.
 */
    public static synchronized int getNumberNormalChannels()
    {
        return numberNormalChannels;
    }
    
/**
 * Sets the maximum number of streaming channels that can be created.  
 * Streaming channels are created first, so the higher the maximum number of 
 * streaming channels is set, the fewer non-streaming channels will 
 * be available.  If unable to create the number of channels specified, 
 * SoundSystem will create as many as possible.
 * NOTE: JavaSound may require the total number of channels (non-streaming + 
 * streaming) to be 32.
 * @param number Maximum streaming channels.
 */
    public static synchronized void setNumberStreamingChannels( int number )
    {
        numberStreamingChannels = number;
    }
    
/**
 * Returns the maximum number of streaming channels that can be created.
 * @return Maximum streaming channels.
 */
    public static synchronized int getNumberStreamingChannels()
    {
        return numberStreamingChannels;
    }
    
/**
 * Sets the varriable used for overall volume, affecting all sources.
 * @param value Float value (0.0f - 1.0f).
 */
    public static synchronized void setMasterGain( float value )
    {
        masterGain = value;
    }
    
/**
 * Returns the value for the overall volume.
 * @return A float value (0.0f - 1.0f).
 */
    public static synchronized float getMasterGain()
    {
        return masterGain;
    }

/**
 * Sets the default attenuation model to use when one is not specified. 
 * Attenuation is how a source's volume fades with distance.
 * @param model A global attenuation model identifier.
 */
    // Use the following methods to interface the private variables above:
    public static synchronized void setDefaultAttenuation( int model )
    {
        defaultAttenuationModel = model;
    }
/**
 * Returns the default attenuation model used when one is not specified.
 * @return A global attenuation model identifier
 */
    public static synchronized int getDefaultAttenuation()
    {
        return defaultAttenuationModel;
    }
/**
 * Sets the default rolloff factor to use when one is not specified.
 * @param rolloff Rolloff factor.
 */
    public static synchronized void setDefaultRolloff( float rolloff )
    {
        defaultRolloffFactor = rolloff;
    }
/**
 * Returns the default rolloff factor used when one is not specified.
 * @return Default rolloff factor
 */
    public static synchronized float getDefaultRolloff()
    {
        return defaultRolloffFactor;
    }
/**
 * Sets the default fade distance to use when one is not specified.
 * @param distance Fade Distance.
 */
    public static synchronized void setDefaultFadeDistance( float distance )
    {
        defaultFadeDistance = distance;
    }
/**
 * Returns the default fade distance used when one is not specified.
 * @return Default fade distance
 */
    public static synchronized float getDefaultFadeDistance()
    {
        return defaultFadeDistance;
    }
/**
 * Sets the package where sound files are located.
 * @param location Path to the sound files location (must be followed by '/').
 */
    public static synchronized void setSoundFilesPackage( String location )
    {
        soundFilesPackage = location;
    }
/**
 * Returns the package where sound files are located.
 * @return Path to the sound files location
 */
    public static synchronized String getSoundFilesPackage()
    {
        return soundFilesPackage;
    }
/**
 * Sets the package where sound files are located.
 * @param location Path to the sound files location (must be followed by '/').
 */
    public static synchronized void setDefaultMidiSoundBank( String location )
    {
        defaultMidiSoundBank = location;
    }
/**
 * Returns the package where sound files are located.
 * @return Path to the sound files location
 */
    public static synchronized String getDefaultMidiSoundBank()
    {
        return defaultMidiSoundBank;
    }
/**
 * Sets the number of bytes to load at a time when streaming.
 * @param size Size in bytes.
 */
    public static synchronized void setStreamingBufferSize( int size )
    {
        streamingBufferSize = size;
    }
/**
 * Returns the number of bytes to load at a time when streaming.
 * @return Size in bytes.
 */
    public static synchronized int getStreamingBufferSize()
    {
        return streamingBufferSize;
    }
/**
 * Sets the number of buffers used for each streaming sorce.
 * @param num How many buffers.
 */
    public static synchronized void setNumberStreamingBuffers( int num )
    {
        numberStreamingBuffers = num;
    }
/**
 * Returns the number of buffers used for each streaming sorce.
 * @return How many buffers.
 */
    public static synchronized int getNumberStreamingBuffers()
    {
        return numberStreamingBuffers;
    }
/**
 * Sets the maximum number of bytes to read in for (non-streaming) files.  
 * Increase this value if non-streaming sounds are getting cut off.  
 * Decrease this value if large sound files are causing lag during load time.  
 * @param size Size in bytes.
 */
    public static synchronized void setMaxFileSize( int size )
    {
        maxFileSize = size;
    }
/**
 * Returns the maximum number of bytes to read in for (non-streaming) files.  
 * @return Size in bytes.
 */
    public static synchronized int getMaxFileSize()
    {
        return maxFileSize;
    }
/**
 * Sets the size of each chunk to read at a time for loading (non-streaming) files.
 * Increase if loading sound files is causing significant lag.
 * @param size Size in bytes.
 */
    public static synchronized void setFileChunkSize( int size )
    {
        fileChunkSize = size;
    }
/**
 * Returns the size of each chunk to read at a time for loading (non-streaming) files.
 * @return Size in bytes.
 */
    public static synchronized int getFileChunkSize()
    {
        return fileChunkSize;
    }
    
//  END STATIC SYNCHRONIZED INTERFACE METHODS
}
