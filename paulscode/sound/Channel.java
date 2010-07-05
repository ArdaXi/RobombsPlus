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

import java.util.LinkedList;

/**
 * The Channel class is the base class which can be extended for 
 * library-specific channels.  It is also used in the "no-sound" library.  
 * A channel is a reserved sound-card voice through which sources are played 
 * back.  Channels can be either streaming channels or normal (non-streaming) 
 * ones.  For consistant naming conventions, each sub-class should have the 
 * name prefix "Channel".
 * 
 * Author: Paul Lamb
 */
public class Channel
{
/**
 * Global identifier for this library type.  This varriable should be set in 
 * an extended class's constructor.  For more information about global library 
 * identifiers, see {@link paulscode.sound.SoundSystemConfig SoundSystemConfig}.
 */
    public int libraryType = SoundSystemConfig.LIBRARY_NOSOUND;
    
/**
 * Global identifier for the type of channel (normal or streaming).  Possible 
 * values for this varriable can be found in the 
 * {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} class.
 */
    public int channelType;

/**
 * Processes status messages, warnings, and error messages.
 */
    private SoundSystemLogger logger;
    
    
/**
 * Constructor:  Takes channelType identifier as a paramater.  Possible values 
 * for channel type can be found in the 
 * {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} class.
 * @param type Type of channel (normal or streaming).
 */
    public Channel( int type )
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        channelType = type;
    }
    
/**
 * Shuts the channel down and removes references to all instantiated objects.
 */
    public void cleanup()
    {
        logger = null;
    }
    
/**
 * Queues up the initial byte[] buffers of data to be streamed.
 * @param bufferList List of the first buffers to be played for a streaming source.
 * @return False if an error occurred or if end of stream was reached.
 */
    public boolean preLoadBuffers( LinkedList<byte[]> bufferList )
    {
        return true;
    }
    
/**
 * Queues up a byte[] buffer of data to be streamed.
 * @param buffer The next buffer to be played for a streaming source.
 * @return False if an error occurred or if the channel is shutting down.
 */
    public boolean queueBuffer( byte[] buffer )
    {
        return false;
    }
    
/**
 * Returns the number of queued byte[] buffers that have finished playing.
 * @return Number of buffers processed.
 */
    public int buffersProcessed()
    {
        return 0;
    }
    
/**
 * Plays the next queued byte[] buffer.  This method is run from the seperate 
 * {@link paulscode.sound.StreamThread StreamThread}.
 * @return False when no more buffers are left to process.
 */
    public boolean processBuffer()
    {
        return false;
    }
    
/**
 * Dequeues all previously queued data.
 */
    public void flush()
    {}
    
/**
 * Stops the channel, dequeues any queued data, and closes the channel.
 */
    public void close()
    {}
    
/**
 * Plays the currently attached normal source, opens this channel up for 
 * streaming, or resumes playback if this channel was paused.
 */
    public void play()
    {}
    
/**
 * Temporarily stops playback for this channel.
 */
    public void pause()
    {}
    
/**
 * Stops playback for this channel and rewinds the attached source to the 
 * beginning.
 */
    public void stop()
    {}
    
/**
 * Rewinds the attached source to the beginning.  Stops the source if it was 
 * paused.
 */
    public void rewind()
    {}
    
/**
 * Used to determine if a channel is actively playing a source.  This method 
 * will return false if the channel is paused or stopped and when no data is 
 * queued to be streamed.
 * @return True if this channel is playing a source.
 */
    public boolean playing()
    {
        return false;
    }
    
/**
 * Returns the name of the class.
 * @return "Channel" + library title.
 */
    public String getClassName()
    {
        if( libraryType == SoundSystemConfig.LIBRARY_NOSOUND )
            return "Channel";
        else
            return "Channel" + 
                    SoundSystemConfig.getLibraryTitle( libraryType );
    }
    
/**
 * Prints a message.
 * @param message Message to print.
 */
    protected void message( String message )
    {
        logger.message( message, 0 );
    }
    
/**
 * Prints an important message.
 * @param message Message to print.
 */
    protected void importantMessage( String message )
    {
        logger.importantMessage( message, 0 );
    }
    
/**
 * Prints the specified message if error is true.
 * @param error True or False.
 * @param message Message to print if error is true.
 * @return True if error is true.
 */
    protected boolean errorCheck( boolean error, String message )
    {
        return logger.errorCheck( error, getClassName(), message, 0 );
    }
    
/**
 * Prints an error message.
 * @param message Message to print.
 */
    protected void errorMessage( String message )
    {
        logger.errorMessage( getClassName(), message, 0 );
    }
    
/**
 * Prints an exception's error message followed by the stack trace.
 * @param e Exception containing the information to print.
 */
    protected void printStackTrace( Exception e )
    {
        logger.printStackTrace( e, 1 );
    }
}
