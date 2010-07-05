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
 * The Source class is used to store information about a source.  
 * Source objects are stored in a map in the Library class.  The 
 * information they contain is used to create library-specific sources.
 * This is the template class which is extended for each specific library.  
 * This class is also used by the "No Sound" library to represent a mute 
 * source.  
 * 
 * Author: Paul Lamb
 */
public class Source
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
 * Global identifier for this library type.  This varriable should be set in 
 * an extended class's constructor.  For more information about global library 
 * identifiers, see {@link paulscode.sound.SoundSystemConfig SoundSystemConfig}.
 */
    public int libraryType = SoundSystemConfig.LIBRARY_NOSOUND;
    
/**
 * Processes status messages, warnings, and error messages.
 */
    private SoundSystemLogger logger;
    
/**
 * Determines whether a source should be removed after it finishes playing.
 */
    public boolean temporary = false;
    
/**
 * Determines whether or not this is a priority source.  Priority sources will 
 * not be overwritten by other sources when there are no available channels.
 */
    public boolean priority = false;
    
/**
 * Whether or not this source should be streamed.
 */
    public boolean toStream = false;
    
/**
 * Whether this source should loop or only play once.
 */
    public boolean toLoop = false;
    
/**
 * Whether this source needs to be played (for example if it was playing and 
 * looping when it got culled).
 */
    public boolean toPlay = false;
    
/**
 * Unique name for this source.  More than one source can not have the same 
 * sourcename.
 */
    public String sourcename = "";
    
/**
 * Name of the audio file which this source should play.
 */
    public String filename = "";
    
/**
 * This source's position in 3D space.
 */
    public Vector3D position;
    
/**
 * Attenuation model to use for this source.
 */
    public int attModel = 0;
    
/**
 * Either fade distance or rolloff factor, depending on the value of attModel.
 */
    public float distOrRoll = 0.0f;
    
/**
 * This source's volume (a float between 0.0 - 1.0).  This value is used 
 * internally for attenuation, and should not be used to manually change a 
 * source's volume.
 */
    public float gain = 1.0f;
    
/**
 * This value should be used to manually increase or decrease source volume.
 */
    public float sourceVolume = 1.0f;
    
/**
 * This source's distance from the listener.
 */
    public float distanceFromListener = 0.0f;
    
/**
 * Channel to play this source on.
 */
    public Channel channel = null;
    
/**
 * False when this source gets culled.
 */
    private boolean active = true;
    
/**
 * Whether or not this source has been stopped.
 */
    private boolean stopped = true;
    
/**
 * Whether or not this source has been paused.
 */
    private boolean paused = false;
    
/**
 * Holds the data used by streaming sources.
 */
    protected AudioData audioData = null;
    
/**
 * current byte position in the audio stream.
 */
    protected int streamPosition = 0;
    
/**
 * Used by streaming sources to indicate whether or not the initial 
 * stream-buffers still need to be queued.
 */
    public boolean preLoad = false;
    
    
/**
 * Constructor:  Creates a new source using the specified parameters.
 * @param pri Setting this to true will prevent other sounds from overriding this one.
 * @param strm Setting this to true will create a streaming source.
 * @param lp Should this source loop, or play only once.
 * @param src A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param fil The name of the sound file to play at this source.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param att Attenuation model to use.
 * @param dr Either the fading distance or rolloff factor, depending on the value of 'att'.
 * @param tmp Whether or not to remove this source after it finishes playing.
 */
    public Source( boolean pri, boolean strm, boolean lp, String src,
                       String fil, AudioData data, float x, float y, float z,
                       int att, float dr, boolean tmp )
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        priority = pri;
        toStream = strm;
        toLoop = lp;
        sourcename = src;
        filename = fil;
        audioData = data;
        position = new Vector3D( x, y, z );
        attModel = att;
        distOrRoll = dr;
        temporary = tmp;
    }
    
/**
 * Constructor:  Creates a new source matching the specified one.
 * @param old Source to copy information from.
 */
    public Source( Source old, AudioData data )
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        priority = old.priority;
        toStream = old.toStream;
        toLoop = old.toLoop;
        sourcename = old.sourcename;
        filename = old.filename;
        position = old.position.clone();
        attModel = old.attModel;
        distOrRoll = old.distOrRoll;
        temporary = old.temporary;
        
        sourceVolume = old.sourceVolume;
        
        audioData = data;
    }
    
/*  Override methods  */
    
/**
 * Shuts the source down and removes references to all instantiated objects.
 */
    public void cleanup()
    {}
    
/**
 * Sets whether or not this source should be removed when it finishes playing.
 * @param tmp True or false.
 */
    public void setTemporary( boolean tmp )
    {
        temporary = tmp;
    }
    
/**
 * Called every time the listener's position or orientation changes.
 */
    public void listenerMoved()
    {}
    
/**
 * Moves the source to the specified position.
 * @param x X coordinate to move to.
 * @param y Y coordinate to move to.
 * @param z Z coordinate to move to.
 */
    public void setPosition( float x, float y, float z )
    {
        position.x = x;
        position.y = y;
        position.z = z;
    }
    
/**
 * Called every time the source changes position.
 */
    public void positionChanged()
    {}
    
/**
 * Sets whether or not this source is a priority source.  A priority source 
 * will not be overritten by another source if there are no channels available 
 * to play on.
 * @param pri True or false.
 */
    public void setPriority( boolean pri )
    {
        priority = pri;
    }
    
/**
 * Sets whether this source should loop or only play once.
 * @param lp True or false.
 */
    public void setLooping( boolean lp )
    {
        toLoop = lp;
    }
    
/**
 * Sets this source's attenuation model.
 * @param model Attenuation model to use.
 */
    public void setAttenuation( int model )
    {
        attModel = model;
    }

/**
 * Sets this source's fade distance or rolloff factor, depending on the 
 * attenuation model.
 * @param dr New value for fade distance or rolloff factor.
 */
    public void setDistOrRoll( float dr)
    {
        distOrRoll = dr;
    }
    
/**
 * Returns the source's distance from the listener.
 * @return How far away the source is.
 */
    public float getDistanceFromListener()
    {
        return distanceFromListener;
    }
    
/**
 * Changes the sources peripheral information to match the supplied parameters. 
 * @param pri Setting this to true will prevent other sounds from overriding this one.
 * @param strm Setting this to true will create a streaming source.
 * @param lp Should this source loop, or play only once.
 * @param src A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param fil The name of the sound file to play at this source.
 * @param x X position for this source.
 * @param y Y position for this source.
 * @param z Z position for this source.
 * @param att Attenuation model to use.
 * @param dr Either the fading distance or rolloff factor, depending on the value of 'att'.
 * @param tmp Whether or not to remove this source after it finishes playing.
 */
    public void changeSource( boolean pri, boolean strm, boolean lp,
                              String src, String fil, AudioData data, float x,
                              float y, float z, int att, float dr, boolean tmp )
    {
        priority = pri;
        toStream = strm;
        toLoop = lp;
        sourcename = src;
        filename = fil;
        audioData = data;
        position.x = x;
        position.y = y;
        position.z = z;
        attModel = att;
        distOrRoll = dr;
        temporary = tmp;
    }
    
/**
 * Plays the source on the specified channel.
 * @param c Channel to play on.
 */
    public void play( Channel c )
    {
        if( !active( GET, XXX) )
        {
            if( toLoop )
                toPlay = true;
            return;
        }
        if( channel != c )
        {
            channel = c;
            channel.close();
        }
        // change the state of this source to not stopped and not paused:
        stopped( SET, false );
        paused( SET, false );
    }    
/*  END Override methods  */
    
/**
 * Streams the source on its current channel
 * @return False when stream has finished playing.
 */
    public boolean stream()
    {
        if( preLoad )
            return preLoad();

        if( channel == null || audioData == null || stopped() )
            return false;
        
        if( audioData.soundBytes == null
            || audioData.soundBytes.array().length == 0 )
            return false;
        
        if( streamPosition >= audioData.soundBytes.array().length )
            return false;
        
        if( paused() )
            return true;
        
        int bytesRemaining;
        byte[] buf = null;
        int processed = channel.buffersProcessed();
        for( int i = 0; i < processed; i++ )
        {
            bytesRemaining = audioData.soundBytes.array().length
                             - streamPosition;
            if( bytesRemaining <= 0 )
                return false;
            
            if( bytesRemaining < SoundSystemConfig.getStreamingBufferSize() )
                buf = new byte[bytesRemaining];
            else
                buf = new byte[SoundSystemConfig.getStreamingBufferSize()];
            try
            {
                System.arraycopy( audioData.soundBytes.array(), streamPosition,
                                  buf, 0, buf.length );
            }
            catch( Exception e )
            {
                buf = null;
                return false;
            }
            
            if( buf == null )
                return false;  // end of stream

            streamPosition += buf.length;

            channel.queueBuffer( buf );
        }
        
        return true;
    }
    
/**
 * Queues up the initial stream-buffers for the stream.
 * @return False if the end of the stream was reached.
 */
    public boolean preLoad()
    {
        return false;
    }
    
/**
 * Pauses the source.
 */
    public void pause()
    {
        toPlay = false;
        paused( SET, true );
        if( channel != null )
            channel.pause();
        else
            errorMessage( "Channel null in method 'pause'" );
    }
    
/**
 * Stops the source.
 */
    public void stop()
    {
        toPlay = false;
        stopped( SET, true );
        paused( SET, false );
        if( channel != null )
            channel.stop();
        else
            errorMessage( "Channel null in method 'stop'" );
    }
    
/**
 * Rewinds the source.  If the source was paused, then it is stopped.
 */
    public void rewind()
    {
        if( paused( GET, XXX ) )
        {
            stop();
        }
        if( channel != null )
        {
            boolean rePlay = playing();
            channel.rewind();
            if( toStream && rePlay )
            {
                stop();
                play( channel );
            }
        }
        else
            errorMessage( "Channel null in method 'rewind'" );
    }    
    
/**
 * Dequeues any previously queued data.
 */
    public void flush()
    {
        if( channel != null )
            channel.flush();
        else
            errorMessage( "Channel null in method 'flush'" );
    }
    
/**
 * Stops and flushes the source, and prevents it from being played again until 
 * the activate() is called.  
 */
    public void cull()
    {
        if( !active( GET, XXX ) )
            return;
        if( playing() && toLoop )
            toPlay = true;
        active( SET, false );
        if( channel != null )
            channel.close();
    }
    
/**
 * Allows a previously culled source to be played again.
 */
    public void activate()
    {
        active( SET, true );
    }
    
/**
 * Returns false if the source has been culled.  
 * @return True or False
 */
    public boolean active()
    {
        return active( GET, XXX );
    }
    
/**
 * Returns true if the source is playing.  
 * @return True or False
 */
    public boolean playing()
    {
        if( channel == null )
            return false;
        else if( paused() || stopped() )
            return false;
        else
            return channel.playing();
    }
    
/**
 * Returns true if the source has been stopped.  
 * @return True or False
 */
    public boolean stopped()
    {
        return stopped( GET, XXX );
    }
    
/**
 * Returns true if the source has been paused.  
 * @return True or False
 */
    public boolean paused()
    {
        return paused( GET, XXX );
    }
    
/**
 * Sets or returns whether or not the source has been culled.  
 * @return True or False
 */
    private synchronized boolean active( boolean action, boolean value )
    {
        if( action == SET )
            active = value;
        return active;
    }
    
/**
 * Sets or returns whether or not the source has been stopped.  
 * @return True or False
 */
    private synchronized boolean stopped( boolean action, boolean value )
    {
        if( action == SET )
            stopped = value;
        return stopped;
    }
    
/**
 * Sets or returns whether or not the source has been paused.  
 * @return True or False
 */
    private synchronized boolean paused( boolean action, boolean value )
    {
        if( action == SET )
            paused = value;
        return paused;
    }
    
/**
 * Returns the name of the class.
 * @return SoundLibraryXXXX.
 */
    public String getClassName()
    {
        if( libraryType == SoundSystemConfig.LIBRARY_NOSOUND )
            return "Source";
        else
            return "Source" + 
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
}
