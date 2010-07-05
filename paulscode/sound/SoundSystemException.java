package paulscode.sound;

/**
 * The SoundSystemException class is used for SoundSystem-specific errors.
 */
public class SoundSystemException extends Exception
{
/**
 * Global identifier for no problem.
 */
    public static final int ERROR_NONE                = 0;
/**
 * Global identifier for a generic exception.
 */
    public static final int UNKNOWN_ERROR             = 1;
/**
 * Global identifier for the sound library does not exist.
 */
    public static final int LIBRARY_NULL              = 2;
/**
 * Global identifier for the sound library does not exist.
 */
    public static final int LIBRARY_TYPE              = 3;
/**
 * Global identifier for an exception during AL.create().  Probably means 
 * that OpenAL is not supported.
 */
    public static final int OPENAL_CREATE             = 4;
/**
 * Global identifier for an invalid name parameter in OpenAL.
 */
    public static final int OPENAL_INVALID_NAME       = 5;
/**
 * Global identifier for an invalid parameter in OpenAL.
 */
    public static final int OPENAL_INVALID_ENUM       = 6;
/**
 * Global identifier for an invalid enumerated parameter value in OpenAL.
 */
    public static final int OPENAL_INVALID_VALUE      = 7;
/**
 * Global identifier for an illegal call in OpenAL.
 */
    public static final int OPENAL_INVALID_OPERATION  = 8;
/**
 * Global identifier for OpenAL out of memory.
 */
    public static final int OPENAL_OUT_OF_MEMORY      = 9;
/**
 * Global identifier for an exception while creating the OpenAL Listener.
 */
    public static final int OPENAL_LISTENER           = 10;
/**
 * Global identifier for unable to find the Java Sound Mixer.  Probably means 
 * that JavaSound audio mixing is not supported.
 */
    public static final int JAVASOUND_MIXER           = 11;
    
/**
 * Holds a global identifier indicating the type of exception.
 */
    private int myType = UNKNOWN_ERROR;
    
/**
 * Constructor: Generic exception.  Specify the error message.
 */
    public SoundSystemException( String message )
    {
        super( message );
    }
    
/**
 * Constructor: Specify the error message and type of exception.
 * @param message Description of the problem.
 * @param type Global identifier for type of exception.
 */
    public SoundSystemException( String message, int type )
    {
        super( message );
        myType = type;
    }
    
    public int getType()
    {
        return myType;
    }
}
