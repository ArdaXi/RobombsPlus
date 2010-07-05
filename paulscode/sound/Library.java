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
import java.util.Set;

/**
 * The Library class is the class from which all library types are extended.  
 * It provides generic methods for interfacing with the audio libraries 
 * supported by the SoundSystem.  Specific libraries should extend this class 
 * and override the necessary methods.  For consistant naming conventions, each 
 * sub-class should have the name prefix "Library".  
 * 
 * This class may also be used as the "No Sound" Library if no other 
 * audio libraries are supported by the host machine, or to mute all sound.
 * 
 * Author: Paul Lamb
 */
public class Library
{
/**
 * Global identifier for this library type.  This varriable should be set in 
 * an extended class's constructor.  For more information about global library 
 * identifiers, see {@link paulscode.sound.SoundSystemConfig SoundSystemConfig}.
 */
    protected int libraryType = SoundSystemConfig.LIBRARY_NOSOUND;
    
/**
 * Processes status messages, warnings, and error messages.
 */
    private SoundSystemLogger logger;
    
/**
 * Position and orientation of the listener.
 */
    protected ListenerData listener;

/**
 * Map containing sound file data for easy lookup by filename.
 */
    protected HashMap<String, AudioData> bufferMap = null;
    
/**
 * Map containing all created sources for easy look-up by name.
 */
    protected HashMap<String, Source> sourceMap;  // (name, source data) pairs

/**
 * Interface through which MIDI files can be played.
 */
    private MidiChannel midiChannel;
    
/**
 * Array containing maximum number of non-streaming audio channels.
 */
    private List<Channel> streamingChannels;
    
/**
 * Array containing maximum number of non-streaming audio channels.
 */
    private List<Channel> normalChannels;
    
/**
 * Source name last played on each streaming channel.
 */
    private String[] streamingChannelSourceNames;
    
/**
 * Source name last played on each non-streaming channel.
 */
    private String[] normalChannelSourceNames;
    
/**
 * Increments through the steaming channel list as new sources are played.
 */
    private int nextStreamingChannel = 0;
    
/**
 * Increments through the non-steaming channel list as new sources are played.
 */
    private int nextNormalChannel = 0;

/**
 * Handles processing for all streaming sources.
 */
    protected StreamThread streamThread;
    
/**
 * Constructor: Instantiates the source map and listener information.  NOTES: 
 * The 'super()' method should be at the top of constructors for all extended 
 * classes.  The varriable 'libraryType' should be given a new value in the 
 * constructors for all extended classes.
 */
    public Library() throws SoundSystemException
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        // instantiate the buffer map:
        bufferMap = new HashMap<String, AudioData>();
        
        // instantiate the source map:
        sourceMap = new HashMap<String, Source>();
        
        listener = new ListenerData( 0.0f, 0.0f, 0.0f,  // position
                                     0.0f, 0.0f, -1.0f, // look-at direction
                                     0.0f, 1.0f, 0.0f,  // up direction
                                     0.0f );            // angle
        
        streamingChannels = new LinkedList<Channel>();
        normalChannels = new LinkedList<Channel>();
        streamingChannelSourceNames = new String[
                               SoundSystemConfig.getNumberStreamingChannels() ];
        normalChannelSourceNames = new String[
                                  SoundSystemConfig.getNumberNormalChannels() ];
        
        streamThread = new StreamThread();
        streamThread.start();
    }
/*  OVERRIDE THE FOLLOWING METHODS */    
    
/**
 * Stops all sources, shuts down sound library, and removes references to all 
 * instantiated objects.
 */
    public void cleanup()
    {
        streamThread.kill();
        streamThread.interrupt();
        
        // wait up to 5 seconds for stream thread to end:
        for( int i = 0; i < 50; i++ )
        {
            if( !streamThread.alive() )
                break;
            try
            {
                Thread.sleep(100);
            }
            catch(Exception e)
            {}
        }
        
        if( streamThread.alive() )
        {
            errorMessage( "Stream thread did not die!" );
            message( "Ignoring errors... continuing clean-up." );
        }
        
        if( midiChannel != null )
        {
            midiChannel.cleanup();
            midiChannel = null;
        }

        Channel channel = null;
        if( streamingChannels != null )
        {
            while( !streamingChannels.isEmpty() )
            {
                channel = streamingChannels.remove(0);
                channel.close();
                channel.cleanup();
                channel = null;
            }
            streamingChannels.clear();
            streamingChannels = null;
        }
        if( normalChannels != null )
        {
            while( !normalChannels.isEmpty() )
            {
                channel = normalChannels.remove(0);
                channel.close();
                channel.cleanup();
                channel = null;
            }
            normalChannels.clear();
            normalChannels = null;
        }
        
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
        sourceMap = null;
        
        listener = null;
        streamThread = null;
    }
    
/**
 * Initializes the sound library.
 */
    public void init() throws SoundSystemException
    {
        Channel channel = null;
        
        // create the streaming channels:
        for( int x = 0; x < SoundSystemConfig.getNumberStreamingChannels(); x++ )
        {
            channel = createChannel( SoundSystemConfig.TYPE_STREAMING );
            if( channel == null )
                break;
            streamingChannels.add( channel );
        }
        // create the non-streaming channels:
        for( int x = 0; x < SoundSystemConfig.getNumberNormalChannels(); x++ )
        {
            channel = createChannel( SoundSystemConfig.TYPE_NORMAL );
            if( channel == null )
                break;
            normalChannels.add( channel );
        }
    }
    
/**
 * Checks if the no-sound library type is compatible.
 * @return True or false.
 */
    public static boolean libraryCompatible()
    {
        return true;  // the no-sound library is always compatible.
    }
    
/**
 * Creates a new channel of the specified type (normal or streaming).  Possible 
 * values for channel type can be found in the 
 * {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} class.
 * @param type Type of channel.
 * @return The new channel.
 */
    protected Channel createChannel( int type )
    {
        return null;
    }
    
/**
 * Returns a handle to the next available channel.  If the specified 
 * source is a normal source, a normal channel is returned, and if it is a 
 * streaming source, then a streaming channel is returned.  If all channels of 
 * the required type are currently playing, then the next channel playing a 
 * non-priority source is returned.  If no channels are available (i.e. they 
 * are all playing priority sources) then getNextChannel returns null.  
 * @param source Source to find a channel for.
 * @return The next available channel, or null.
 */
    private Channel getNextChannel( Source source )
    {
        if( source == null )
            return null;
        
        String sourcename = source.sourcename;
        if( sourcename == null )
            return null;
        
        int x;
        int channels;
        int nextChannel;
        List<Channel> channelList;
        String[] sourceNames;
        String name;
        
        if( source.toStream )
        {
            nextChannel = nextStreamingChannel;
            channelList = streamingChannels;
            sourceNames = streamingChannelSourceNames;
        }
        else
        {
            nextChannel = nextNormalChannel;
            channelList = normalChannels;
            sourceNames = normalChannelSourceNames;
        }
        
        channels = channelList.size();
        
        // Check if this source is already on a channel:
        for( x = 0; x < channels; x++ )
        {
            if( sourcename.equals( sourceNames[x] ) )
                return channelList.get( x );
        }
        
        int n = nextChannel;
        Source src;
        // Play on the next new or non-playing channel:
        for( x = 0; x < channels; x++ )
        {
            name = sourceNames[n];
            if( name == null )
                src = null;
            else
                src = sourceMap.get( name );
            
            if( src == null || !src.playing() )
            {
                if( source.toStream )
                {
                    nextStreamingChannel = n + 1;
                    if( nextStreamingChannel >= channels )
                        nextStreamingChannel = 0;
                }
                else
                {
                    nextNormalChannel = n + 1;
                    if( nextNormalChannel >= channels )
                        nextNormalChannel = 0;
                }
                sourceNames[n] = sourcename;
                return channelList.get( n );
            }
            n++;
            if( n >= channels )
                n = 0;
        }
        
        n = nextChannel;
        // Play on the next non-priority channel:
        for( x = 0; x < channels; x++ )
        {
            name = sourceNames[n];
            if( name == null )
                src = null;
            else
                src = sourceMap.get( name );
            
            if( src == null || !src.playing() || !src.priority )
            {
                if( source.toStream )
                {
                    nextStreamingChannel = n + 1;
                    if( nextStreamingChannel >= channels )
                        nextStreamingChannel = 0;
                }
                else
                {
                    nextNormalChannel = n + 1;
                    if( nextNormalChannel >= channels )
                        nextNormalChannel = 0;
                }
                sourceNames[n] = sourcename;
                return channelList.get( n );
            }
            n++;
            if( n >= channels )
                n = 0;
        }
        
        return null;
    }
    
/**
 * Pre-loads a sound into memory.  The file may either be located within the 
 * JAR or at an online location.  If the file is online, filename must begin 
 * with "http://", since that is how SoundSystem recognizes URL's.  If the file 
 * is located within the compiled JAR, the package in which sound files are 
 * located may be set by calling 
 * SoundSystemConfig.setSoundFilesPackage( String ).  
 * @param filename Sound file to load.
 * @return True if the sound loaded properly.
 */
    public boolean loadSound( String filename )
    {
        return true;
    }
    
/**
 * Removes a pre-loaded sound from memory.  This is a good method to use for 
 * freeing up memory after a large sound file is no longer needed.  NOTE: the 
 * source will remain in memory after this method has been called, for as long 
 * as the sound is attached to an existing source.
 * @param filename Sound file to unload.
 */
    public void unloadSound( String filename )
    {
        bufferMap.remove( filename );
    }
    
/**
 * Loads the specified MIDI file, and saves the source information about it.
 * @param toLoop Midi file should loop or play once.
 * @param sourcename Source identifier.
 * @param filename Sound file to load.
 */
    public void loadMidi( boolean toLoop, String sourcename, String filename )
    {
        if( filename == null || filename.equals( "" ) )
        {
            errorMessage( "Filename not specified in method 'loadMidi'." );
            return;
        }
        
        if( !filename.matches( SoundSystemConfig.EXTENSION_MIDI ) )
        {
            errorMessage( "Filename doesn't end in '.mid' or '.midi' in " +
                          "method loadMidi." );
            return;
        }
        
        if( midiChannel == null )
        {
            midiChannel = new MidiChannel( toLoop, sourcename, filename );
        }
        else
        {
            midiChannel.switchSource( toLoop, sourcename, filename );
        }
    }
    
/**
 * Unloads the current Midi file.
 */
    public void unloadMidi()
    {
        if( midiChannel != null )
            midiChannel.cleanup();
        midiChannel = null;
    }
    
/**
 * Checks if the sourcename matches the midi source.
 * @param sourcename Source identifier.
 * @return True if sourcename and midi sourcename match.
 */
    public boolean midiSourcename( String sourcename )
    {
        if( midiChannel == null || sourcename == null )
            return false;
        
        if( midiChannel.getSourcename() == null || sourcename.equals( "" ) )
            return false;
        
        if( sourcename.equals( midiChannel.getSourcename() ) )
            return true;
        
        return false;
    }
    
/**
 * Creates a new source using the specified information.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param toStream Setting this to true will load the sound in pieces rather than all at once.
 * @param toLoop Should this source loop, or play only once.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param posX X position for this source.
 * @param posY Y position for this source.
 * @param posZ Z position for this source.
 * @param attModel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */
    public void newSource( boolean priority, boolean toStream, boolean toLoop,
                           String sourcename, String filename, float posX,
                           float posY, float posZ, int attModel,
                           float distOrRoll )
    {
        sourceMap.put( sourcename,
                       new Source( priority, toStream, toLoop, sourcename,
                                       filename, null, posX, posY, posZ,
                                       attModel, distOrRoll, false ) );
    }
    
/**
 * Creates and immediately plays a new source that will be removed when it 
 * finishes playing.
 * @param priority Setting this to true will prevent other sounds from overriding this one.
 * @param toStream Setting this to true will load the sound in pieces rather than all at once.
 * @param toLoop Should this source loop, or play only once.
 * @param sourcename A unique identifier for this source.  Two sources may not use the same sourcename.
 * @param filename The name of the sound file to play at this source.
 * @param posX X position for this source.
 * @param posY Y position for this source.
 * @param posZ Z position for this source.
 * @param attModel Attenuation model to use.
 * @param distOrRoll Either the fading distance or rolloff factor, depending on the value of "attmodel".
 */
    public void quickPlay( boolean priority, boolean toStream, boolean toLoop,
                           String sourcename, String filename, float posX,
                           float posY, float posZ, int attModel,
                           float distOrRoll, boolean tmp )
    {
        sourceMap.put( sourcename,
                       new Source( priority, toStream, toLoop, sourcename,
                                       filename, null, posX, posY, posZ,
                                       attModel, distOrRoll, tmp ) );
    }
    
/**
 * 
 * Returns the Source object identified by the specified name.  
 * @param sourcename The source's name.
 * @return The source, or null if not found.
 */
    public Source getSource( String sourcename )
    {
        return sourceMap.get( sourcename );
    }
    
/**
 * 
 * Returns a handle to the MIDI channel, or null if one does not exist.  
 * @return The MIDI channel.
 */
    public MidiChannel getMidiChannel()
    {
        return midiChannel;
    }
    
/**
 * 
 * Specifies the MIDI channel to use.  
 * @param c New MIDI channel.
 */
    public void setMidiChannel( MidiChannel c )
    {
        if( midiChannel != null && midiChannel != c )
            midiChannel.cleanup();
        
        midiChannel = c;
    }
    
/**
 * 
 * Defines whether or not the source should be removed after it finishes 
 * playing.  
 * @param sourcename The source's name.
 * @param temporary True or False.
 */
    public void setTemporary( String sourcename, boolean temporary )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.setTemporary( temporary );
    }
    
/**
 * Changes the specified source's position.
 * @param sourcename The source's name.
 * @param x Destination X coordinate.
 * @param y Destination Y coordinate.
 * @param z Destination Z coordinate.
 */
    public void setPosition( String sourcename, float x, float y, float z )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.setPosition( x, y, z );
    }
    
/**
 * Sets the specified source's priority factor.  A priority source will not be 
 * overriden if there are too many sources playing at once.
 * @param sourcename The source's name.
 * @param pri True or False.
 */
    public void setPriority( String sourcename, boolean pri )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.setPriority( pri );
    }
    
/**
 * Sets the specified source's looping parameter.  If parameter lp is false, 
 * the source will play once and stop.
 * @param sourcename The source's name.
 * @param lp True or False.
 */
    public void setLooping( String sourcename, boolean lp )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.setLooping( lp );
    }
    
/**
 * Sets the specified source's attenuation model. 
 * @param sourcename The source's name.
 * @param model Attenuation model to use.
 */
    public void setAttenuation( String sourcename, int model )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.setAttenuation( model );
    }
    
/**
 * Sets the specified source's fade distance or rolloff factor. 
 * @param sourcename The source's name.
 * @param dr Fade distance or rolloff factor.
 */
    public void setDistOrRoll( String sourcename, float dr)
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.setDistOrRoll( dr );
    }
    
/**
 * Looks up the specified source and plays it.  
 * @param sourcename Name of the source to play.
 */
    public void play( String sourcename )
    {
        if( sourcename == null || sourcename.equals( "" ) )
        {
            errorMessage( "Sourcename not specified in method 'play'" );
            return;
        }
        
        if( midiSourcename( sourcename ) )
        {
            midiChannel.play();
        }
        else
        {
            Source source = sourceMap.get( sourcename );
            play( source );
        }
    }
    
/**
 * Plays the specified source. 
 * @param source The source to play.
 */
    public void play( Source source )
    {
        if( source == null )
            return;
        
        if( !source.playing() )
        {
            Channel channel = getNextChannel( source );

            if( source != null && channel != null )
            {
                source.play( channel );
                if( source.toStream )
                {
                    streamThread.watch( source );
                    streamThread.interrupt();
                }
            }
        }
    }
    
/**
 * Stops the specified source. 
 * @param sourcename The source's name.
 */
    public void stop( String sourcename )
    {
        if( sourcename == null || sourcename.equals( "" ) )
        {
            errorMessage( "Sourcename not specified in method 'stop'" );
            return;
        }
        if( midiSourcename( sourcename ) )
        {
            midiChannel.stop();
        }
        else
        {
            Source mySource = sourceMap.get( sourcename );
            if( mySource != null )
                mySource.stop();
        }
    }
    
/**
 * Pauses the specified source. 
 * @param sourcename The source's name.
 */
    public void pause( String sourcename )
    {
        if( sourcename == null || sourcename.equals( "" ) )
        {
            errorMessage( "Sourcename not specified in method 'stop'" );
            return;
        }
        if( midiSourcename( sourcename ) )
        {
            midiChannel.pause();
        }
        else
        {
            Source mySource = sourceMap.get( sourcename );
            if( mySource != null )
                mySource.pause();
        }
    }
    
/**
 * Rewinds the specified source. 
 * @param sourcename The source's name.
 */
    public void rewind( String sourcename )
    {
        if( midiSourcename( sourcename ) )
        {
            midiChannel.rewind();
        }
        else
        {
            Source mySource = sourceMap.get( sourcename );
            if( mySource != null )
                mySource.rewind();
        }
    }
    
/**
 * Culls the specified source.  A culled source will not play until it has been 
 * activated again.
 * @param sourcename The source's name.
 */
    public void cull( String sourcename )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.cull();
    }
    
/**
 * Activates a previously culled source, so it can be played again.  
 * @param sourcename The source's name.
 */
    public void activate( String sourcename )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.activate();
    }

/**
 * Manually sets the specified source's volume.  
 * @param sourcename The source's name.
 * @param volume A float value ( 0.0f - 1.0f ).
 */
    public void setVolume( String sourcename, float value )
    {
        if( midiSourcename( sourcename ) )
        {
            midiChannel.setVolume( value );
        }
        else
        {
            Source mySource = sourceMap.get( sourcename );
            if( mySource != null )
            {
                float newVolume = value;
                if( newVolume < 0.0f )
                    newVolume = 0.0f;
                else if( newVolume > 1.0f )
                    newVolume = 1.0f;

                mySource.sourceVolume = newVolume;
                mySource.positionChanged();
            }
        }
    }
    
/**
 * Returns the current volume of the specified source, or zero if the specified 
 * source was not found.
 * @param sourcename Source to read volume from.
 * @return Float value representing the source volume (0.0f - 1.0f).
 */
    public float getVolume( String sourcename )
    {
        if( midiSourcename( sourcename ) )
        {
            return midiChannel.getVolume();
        }
        else
        {
            Source mySource = sourceMap.get( sourcename );
            if( mySource != null )
                return mySource.sourceVolume;
            else
                return 0.0f;
        }
    }
    
/**
 * Plays all sources whose 'toPlay' varriable is true but are not currently 
 * playing (such as sources which were culled while looping and then 
 * reactivated).
 */
    public void replaySources()
    {
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
            {
                if( source.toPlay && !source.playing() )
                {
                    play( sourcename );
                    source.toPlay = false;
                }
            }
        }
    }
    
/**
 * Moves the listener relative to the current position. 
 * @param x X offset.
 * @param y Y offset.
 * @param z Z offset.
 */
    public void moveListener( float x, float y, float z )
    {
        setListenerPosition( listener.position.x + x, listener.position.y + y,
                             listener.position.z + z );
    }
    
/**
 * Changes the listener's position. 
 * @param x Destination X coordinate.
 * @param y Destination Y coordinate.
 * @param z Destination Z coordinate.
 */
    public void setListenerPosition( float x, float y, float z )
    {
        // update listener's position
        listener.setPosition( x, y, z );
        
        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source source;
        
        // loop through and update the volume of all sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = sourceMap.get( sourcename );
            if( source != null )
                source.positionChanged();
        }
    }
    
/**
 * Turn the listener 'angle' radians counterclockwise around the y-Axis, 
 * relative to the current angle.
 * @param angle Angle in radians.
 */
    public void turnListener( float angle )
    {
        setListenerAngle( listener.angle + angle );
    }
    
/**
 * Changes the listeners orientation to the specified 'angle' radians 
 * counterclockwise around the y-Axis.
 * @param angle Angle in radians.
 */
    public void setListenerAngle( float angle )
    {
        listener.setAngle( angle );
        
        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source source;
        
        // loop through and update the volume of all sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = sourceMap.get( sourcename );
            if( source != null )
                source.positionChanged();
        }
    }
    
/**
 * Changes the listeners orientation using the specified coordinates.
 * @param lookX X element of the look-at direction.
 * @param lookY Y element of the look-at direction.
 * @param lookZ Z element of the look-at direction.
 * @param upX X element of the up direction.
 * @param upY Y element of the up direction.
 * @param upZ Z element of the up direction.
 */
    public void setListenerOrientation( float lookX, float lookY, float lookZ,
                                        float upX, float upY, float upZ )
    {
        listener.setOrientation( lookX, lookY, lookZ, upX, upY, upZ );
        
        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source source;
        
        // loop through and update the volume of all sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = sourceMap.get( sourcename );
            if( source != null )
                source.positionChanged();
        }
    }

/**
 * Changes the listeners position and orientation using the specified listener 
 * data.
 * @param l Listener data to use.
 */
    public void setListenerData( ListenerData l )
    {
        listener.setData( l );
    }
    
/**
 * Creates sources based on the source map provided.
 * @param srcMap Sources to copy.
 */
    public void copySources( HashMap<String, Source> srcMap )
    {
        if( srcMap == null )
            return;
        Set<String> keys = srcMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source srcData;
        
        // remove any existing sources before starting:
        sourceMap.clear();
        
        // loop through and copy all the sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            srcData = srcMap.get( sourcename );
            if( srcData != null )
            {
                loadSound( srcData.filename );
                sourceMap.put( sourcename, new Source( srcData, null ) );
            }
        }
    }
    
/*  END OVERRIDE METHODS */    
    
/**
 * Stops and deletes the specified source. 
 * @param sourcename The source's name.
 */
    public void removeSource( String sourcename )
    {
        Source mySource = sourceMap.get( sourcename );
        if( mySource != null )
            mySource.cleanup(); // end the source, free memory
        sourceMap.remove( sourcename );
    }
    
/**
 * Searches for and removes all temporary sources that have finished playing.
 */
    public void removeTemporarySources()
    {
        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source srcData;
        
        // loop through and cleanup all the sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            srcData = sourceMap.get( sourcename );
            if( (srcData != null) && (srcData.temporary)
                 && (!srcData.playing()) )
            {
                srcData.cleanup(); // end the source, free memory
                iter.remove();
            }
        }
    }
    
/**
 * Tells all the sources that the listener has moved.
 */
    public void listenerMoved()
    {
        Set<String> keys = sourceMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source srcData;
        
        // loop through and copy all the sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            srcData = sourceMap.get( sourcename );
            if( srcData != null )
            {
                srcData.listenerMoved();
            }
        }
    }
    
/**
 * Sets the overall volume to the specified value, affecting all sources.
 * @param value New volume, float value ( 0.0f - 1.0f ).
 */    
    public void setMasterVolume( float value )
    {
        SoundSystemConfig.setMasterGain( value );
        if( midiChannel != null )
            midiChannel.checkVolume();
    }
    
/**
 * Returns the sources map.
 * @return Map of all sources.
 */
    public HashMap<String, Source> getSources()
    {
        return sourceMap;
    }
    
/**
 * Returns information about the listener.
 * @return A ListenerData object.
 */
    public ListenerData getListenerData()
    {
        return listener;
    }
    
/**
 * Returns the current active library.
 * @return Global library identifier.
 */
    public int getType()
    {
        return libraryType;
    }
    
/**
 * Returns the title of the current active library.
 * @return A short title.
 */
    public String getTitle()
    {
        return SoundSystemConfig.getLibraryTitle( libraryType );
    }
    
/**
 * Returns a description of the current active library.
 * @return A longer description.
 */
    public String getDescription()
    {
        return SoundSystemConfig.getLibraryDescription( libraryType );
    }
    
/**
 * Returns the name of the class.
 * @return "Library" + library title.
 */
    public String getClassName()
    {
        if( libraryType == SoundSystemConfig.LIBRARY_NOSOUND )
            return "SoundLibrary";
        else
            return "SoundLibrary" + 
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
