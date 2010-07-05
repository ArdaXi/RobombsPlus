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

import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

// From the lwjgl library, http://www.lwjgl.org
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

/**
 * The LibraryOpenAL class interfaces the lwjgl binding of OpenAL.  
 * For more information about lwjgl, please visit http://www.lwjgl.org 
 * 
 * Author: Paul Lamb
 */
public class LibraryOpenAL extends Library
{
/**
 * Position of the listener in 3D space.
 */
    private FloatBuffer listenerPositionAL = null;
/**
 * Information about the listener's orientation.
 */
    private FloatBuffer listenerOrientation = null;
/**
 * Velocity of the listener.
 */
    private FloatBuffer listenerVelocity = null;
/**
 * Map containing OpenAL identifiers for sound buffers.
 */
    private HashMap<String, IntBuffer> ALBufferMap = null;
    
/**
 * Constructor: Instantiates the source map, buffer map and listener 
 * information.  Also sets the library type to 
 * SoundSystemConfig.LIBRARY_OPENAL
 */
    public LibraryOpenAL() throws SoundSystemException
    {
        super();
        libraryType = SoundSystemConfig.LIBRARY_OPENAL;
        ALBufferMap = new HashMap<String, IntBuffer>();
    }
    
/**
 * Initializes OpenAL, creates the listener, and grabs up audio channels. 
 */
    @Override
    public void init() throws SoundSystemException
    {
        boolean errors = false; // set to 'true' if error(s) occur:
        
        try
        {
            // Try and create the sound system:
            AL.create();
        }
        catch( LWJGLException e )
        {
            // There was an exception
            errorMessage( "Inable to initialize OpenAL.  Probable cause: " +
                          "OpenAL not supported." );
            printStackTrace( e );
            throw new SoundSystemException( e.getMessage(),
                                           SoundSystemException.OPENAL_CREATE );
        }
        
        errors = errors || checkALError();
        
        // Let user know if the library loaded properly
        if( errors )
            importantMessage( "OpenAL did not initialize properly!" );
        else
            message( "OpenAL initialized." );
        
        // Listener is at the origin, facing along the z axis, no velocity:
        listenerPositionAL = BufferUtils.createFloatBuffer( 3 ).put( 
            new float[] { listener.position.x,
                          listener.position.y,
                          listener.position.z } );
        listenerOrientation = BufferUtils.createFloatBuffer( 6 ).put (
            new float[] { listener.lookAt.x, listener.lookAt.y,
                          listener.lookAt.z, listener.up.x, listener.up.y, 
                          listener.up.z } );
        listenerVelocity = BufferUtils.createFloatBuffer( 3 ).put (
            new float[] { 0.0f, 0.0f, 0.0f } );
        
        // Flip the buffers, so they can be used:
        listenerPositionAL.flip();
        listenerOrientation.flip();
        listenerVelocity.flip();
        
        // Pass the buffers to the sound system, and check for potential errors:
        AL10.alListener( AL10.AL_POSITION, listenerPositionAL );
        errors = errors || checkALError();
        AL10.alListener( AL10.AL_ORIENTATION, listenerOrientation );
        errors = errors || checkALError();
        AL10.alListener( AL10.AL_VELOCITY, listenerVelocity );        
        errors = errors || checkALError();

        // Let user know what caused the above error messages:
        if( errors )
        {
            importantMessage( "OpenAL did not initialize properly!" );
            throw new SoundSystemException( "Problem encountered while " +
                                           "loading OpenAL or creating the " +
                                           "listener.  Probably cause:  " +
                                           "OpenAL not supported",
                                           SoundSystemException.OPENAL_CREATE );
        }
        
        super.init();
    }
    
/**
 * Checks if the OpenAL library type is compatible.
 * @return True or false.
 */
    public static boolean libraryCompatible()
    {
        if( AL.isCreated() )
            return true;
        
        try
        {
            AL.create();
        }
        catch( Exception e )
        {
            return false;
        }
        
        try
        {
            AL.destroy();
        }
        catch( Exception e )
        {}
        
        return true;
    }
    
/**
 * Creates a new channel of the specified type (normal or streaming).  Possible 
 * values for channel type can be found in the 
 * {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} class.
 * @param type Type of channel.
 */
    @Override
    protected Channel createChannel( int type )
    {
        ChannelOpenAL channel;
        IntBuffer ALSource;
        
        ALSource = BufferUtils.createIntBuffer( 1 );
        try
        {
            AL10.alGenSources( ALSource );
        }
        catch( Exception e )
        {
            return null;  // no more voices left
        }
        channel = new ChannelOpenAL( type, ALSource );
        return channel;
    }
    
 /**
 * Stops all sources, shuts down OpenAL, and removes references to all 
 * instantiated objects.
 */
    @Override
    public void cleanup()
    {
        super.cleanup();
        
        IntBuffer channel;
        Set<String> keys = bufferMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String filename;
        IntBuffer buffer;
        
        // loop through and clear all sound buffers:
        while( iter.hasNext() )
        {
            filename = iter.next();
            buffer = ALBufferMap.get( filename );
            if( buffer != null )
            {
                AL10.alDeleteBuffers( buffer );
                checkALError();
                buffer.clear();
            }
        }
        
        bufferMap.clear();
        AL.destroy();
        
        bufferMap = null;
        listenerPositionAL = null;
        listenerOrientation = null;
        listenerVelocity = null;
    }

/**
 * Pre-loads a sound into memory.  The file may either be located within the 
 * JAR or at an online location.  If the file is online, filename must begin 
 * with "http://", since that is how SoundSystem recognizes URL's.  If the file 
 * is located within the compiled JAR, the package in which sound files are 
 * located may be set by calling SoundSystemConfig.setSoundFilesPackage().
 * @param filename Sound file to load.
 * @return True if the sound loaded properly.
 */
    @Override
    public boolean loadSound( String filename )
    {
        // Make sure the buffer map exists:
        if( bufferMap == null )
        {
            bufferMap = new HashMap<String, AudioData>();
            importantMessage( "Buffer Map was null in method 'loadSound'" );
        }
        // Make sure the OpenAL buffer map exists:
        if( ALBufferMap == null )
        {
            ALBufferMap = new HashMap<String, IntBuffer>();
            importantMessage( "Open AL Buffer Map was null in method" +
                              "'loadSound'" );
        }
        
        // make sure they gave us a filename:
        if( errorCheck( filename == null,
                              "Filename not specified in method 'loadSound'" ) )
            return false;
        
        // check if it is already loaded:        
        if( bufferMap.get( filename ) != null )
            return true;
        
        Loader loader = new Loader( filename );
        AudioData audioData = loader.load( true );
        loader.cleanup();
        loader = null;
        if( audioData != null )
            bufferMap.put( filename, audioData );
        else
            errorMessage( "Audio Data null in method 'loadSound'" );
        
        int soundFormat = 0;
        if( audioData.audioFormat.getChannels() == 1 )
        {
            if( audioData.audioFormat.getSampleSizeInBits() == 8 )
            {
                soundFormat = AL10.AL_FORMAT_MONO8;
            }
            else if( audioData.audioFormat.getSampleSizeInBits() == 16 )
            {
                soundFormat = AL10.AL_FORMAT_MONO16;
            }
            else
            {
                errorMessage( "Illegal sample size in method 'loadSound'" );
                return false;
            }
        }
        else if( audioData.audioFormat.getChannels() == 2 )
        {
            if( audioData.audioFormat.getSampleSizeInBits() == 8 )
            {
                soundFormat = AL10.AL_FORMAT_STEREO8;
            }
            else if( audioData.audioFormat.getSampleSizeInBits() == 16 )
            {
                soundFormat = AL10.AL_FORMAT_STEREO16;
            }
            else
            {
                errorMessage( "Illegal sample size in method 'loadSound'" );
                return false;
            }
        }
        else
        {
            errorMessage( "File neither mono nor stereo in method " +
                          "'loadSound'" );
            return false;
        }
        IntBuffer buffer = BufferUtils.createIntBuffer( 1 );
        AL10.alGenBuffers( buffer );
        if( errorCheck( AL10.alGetError() != AL10.AL_NO_ERROR, 
                        "alGenBuffers error when loading " + filename ) )
            return false;
        
        AL10.alBufferData( buffer.get( 0 ), soundFormat, audioData.soundBytes,
                           (int) audioData.audioFormat.getSampleRate() );
        if( errorCheck( AL10.alGetError() != AL10.AL_NO_ERROR, 
                        "alBufferData error when loading " + filename ) )
        
                
        if( errorCheck( buffer == null, 
                        "Sound buffer was not created for " + filename ) )
            return false;
        
        ALBufferMap.put( filename, buffer );
        
        return true;
    }
    
/**
 * Removes a pre-loaded sound from memory.  This is a good method to use for 
 * freeing up memory after a large sound file is no longer needed.  NOTE: the 
 * source will remain in memory after this method has been called, for as long 
 * as the sound is attached to an existing source.
 * @param filename Sound file to unload.
 */
    @Override
    public void unloadSound( String filename )
    {
        ALBufferMap.remove( filename );
        super.unloadSound( filename );
    }
    
 /**
 * Sets the overall volume to the specified value, affecting all sources.
 * @param value New volume, float value ( 0.0f - 1.0f ).
 */ 
    @Override
    public void setMasterVolume( float value )
    {
        super.setMasterVolume( value );
        
        AL10.alListenerf( AL10.AL_GAIN, value );
        checkALError();
    }
    
/**
 * Creates a new source and places it into the source map.
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
    @Override
    public void newSource( boolean priority, boolean toStream, boolean toLoop,
                           String sourcename, String filename, float posX,
                           float posY, float posZ, int attModel,
                           float distOrRoll )
    {
        IntBuffer myBuffer = null;
        if( !toStream )
        {
            // Grab the sound buffer for this file:
            myBuffer = ALBufferMap.get( filename );
            
            // if not found, try loading it:
            if( myBuffer == null )
            {
                if( !loadSound( filename ) )
                {
                    errorMessage( "Source '" + sourcename + "' was not created "
                                  + "because an error occurred while loading "
                                  + filename );
                    return;
                }
            }

            // try and grab the sound buffer again:
            myBuffer = ALBufferMap.get( filename );
            // see if it was there this time:
            if( myBuffer == null )
            {
                errorMessage( "Source '" + sourcename + "' was not created "
                              + "because a sound buffer was not found for "
                              + filename );
                return;
            }
        }
        AudioData audioData = null;
        
        // Grab the audio data for this file:
        audioData = bufferMap.get( filename );
        // if not found, try loading it:
        if( audioData == null )
        {
            if( !loadSound( filename ) )
            {
                errorMessage( "Source '" + sourcename + "' was not created "
                              + "because an error occurred while loading "
                              + filename );
                return;
            }
        }
        // try and grab the sound buffer again:
        audioData = bufferMap.get( filename );
        // see if it was there this time:
        if( audioData == null )
        {
            errorMessage( "Source '" + sourcename + "' was not created "
                          + "because audio data was not found for "
                          + filename );
            return;
        }
        
        sourceMap.put( sourcename,
                       new SourceOpenAL( listenerPositionAL, myBuffer,
                                         priority, toStream, toLoop,
                                         sourcename, filename, audioData, posX,
                                         posY, posZ, attModel, distOrRoll,
                                         false ) );
    }
    
/**
 * Creates and immediately plays a new source.
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
 * @param temporary Whether or not this source should be removed after it finishes playing.
 */
    @Override
    public void quickPlay( boolean priority, boolean toStream, boolean toLoop,
                           String sourcename, String filename, float posX,
                           float posY, float posZ, int attModel,
                           float distOrRoll, boolean temporary )
    {
        IntBuffer myBuffer = null;
        if( !toStream )
        {
            // Grab the sound buffer for this file:
            myBuffer = ALBufferMap.get( filename );
            // if not found, try loading it:
            if( myBuffer == null )
                loadSound( filename );
            // try and grab the sound buffer again:
            myBuffer = ALBufferMap.get( filename );
            // see if it was there this time:
            if( myBuffer == null )
            {
                errorMessage( "Sound buffer was not created for " + filename );
                return;
            }
        }
        
        AudioData audioData = null;
        
        // Grab the audio data for this file:
        audioData = bufferMap.get( filename );
        // if not found, try loading it:
        if( audioData == null )
        {
            if( !loadSound( filename ) )
            {
                errorMessage( "Source '" + sourcename + "' was not created "
                              + "because an error occurred while loading "
                              + filename );
                return;
            }
        }
        // try and grab the sound buffer again:
        audioData = bufferMap.get( filename );
        // see if it was there this time:
        if( audioData == null )
        {
            errorMessage( "Source '" + sourcename + "' was not created "
                          + "because audio data was not found for "
                          + filename );
            return;
        }
        
        sourceMap.put( sourcename,
                       new SourceOpenAL( listenerPositionAL, myBuffer,
                                             priority, toStream, toLoop,
                                             sourcename, filename, audioData,
                                             posX, posY, posZ, attModel,
                                             distOrRoll, temporary ) );
    }
    
/**
 * Creates sources based on the source map provided.
 * @param srcMap Sources to copy.
 */
    @Override
    public void copySources( HashMap<String, Source> srcMap )
    {
        if( srcMap == null )
            return;
        Set<String> keys = srcMap.keySet();
        Iterator<String> iter = keys.iterator();        
        String sourcename;
        Source source;
        
        // Make sure the buffer map exists:
        if( bufferMap == null )
        {
            bufferMap = new HashMap<String, AudioData>();
            importantMessage( "Buffer Map was null in method 'copySources'" );
        }
        // Make sure the OpenAL buffer map exists:
        if( ALBufferMap == null )
        {
            ALBufferMap = new HashMap<String, IntBuffer>();
            importantMessage( "Open AL Buffer Map was null in method" +
                              "'copySources'" );
        }
        
        // remove any existing sources before starting:
        sourceMap.clear();
        
        // loop through and copy all the sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = srcMap.get( sourcename );
            if( source != null )
            {
                loadSound( source.filename );
                sourceMap.put( sourcename, new SourceOpenAL(
                                           listenerPositionAL,
                                           ALBufferMap.get( source.filename ),
                                           source,
                                           bufferMap.get( source.filename ) ) );
            }
        }
    }
    
/**
 * Changes the listener's position. 
 * @param x Destination X coordinate.
 * @param y Destination Y coordinate.
 * @param z Destination Z coordinate.
 */
    @Override
    public void setListenerPosition( float x, float y, float z )
    {
        super.setListenerPosition( x, y, z );
        
        listenerPositionAL.put( 0, x );
        listenerPositionAL.put( 1, y );
        listenerPositionAL.put( 2, z );
        
        // Update OpenAL listener position:
        AL10.alListener( AL10.AL_POSITION, listenerPositionAL );
        // Check for errors:
        checkALError();
    }
    
/**
 * Changes the listeners orientation to the specified 'angle' radians 
 * counterclockwise around the y-Axis.
 * @param angle Radians.
 */
    @Override
    public void setListenerAngle( float angle )
    {
        super.setListenerAngle( angle );
        
        listenerOrientation.put( 0, listener.lookAt.x );
        listenerOrientation.put( 2, listener.lookAt.z );
        
        // Update OpenAL listener orientation:
        AL10.alListener( AL10.AL_ORIENTATION, listenerOrientation );
        // Check for errors:
        checkALError();
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
    @Override
    public void setListenerOrientation( float lookX, float lookY, float lookZ,
                                        float upX, float upY, float upZ )
    {
        super.setListenerOrientation( lookX, lookY, lookZ, upX, upY, upZ );
        listenerOrientation.put( 0, lookX );
        listenerOrientation.put( 1, lookY );
        listenerOrientation.put( 2, lookZ );
        listenerOrientation.put( 3, upX );
        listenerOrientation.put( 4, upY );
        listenerOrientation.put( 5, upZ );
        AL10.alListener( AL10.AL_ORIENTATION, listenerOrientation );
        checkALError();
    }
    
/**
 * Changes the listeners position and orientation using the specified listener 
 * data.
 * @param l Listener data to use.
 */
    @Override
    public void setListenerData( ListenerData l )
    {
        super.setListenerData( l );
        
        listenerPositionAL.put( 0, l.position.x );
        listenerPositionAL.put( 1, l.position.y );
        listenerPositionAL.put( 2, l.position.z );
        AL10.alListener( AL10.AL_POSITION, listenerPositionAL );

        listenerOrientation.put( 0, l.lookAt.x );
        listenerOrientation.put( 1, l.lookAt.y );
        listenerOrientation.put( 2, l.lookAt.z );
        listenerOrientation.put( 3, l.up.x );
        listenerOrientation.put( 4, l.up.y );
        listenerOrientation.put( 5, l.up.z );
        AL10.alListener( AL10.AL_ORIENTATION, listenerOrientation );
        
        checkALError();
    }

/**
 * Checks for OpenAL errors, and prints a message if there is an error.
 * @return True if there was an error, False if not.
 */
    private boolean checkALError()
    {
        switch( AL10.alGetError() )
        {
            case AL10.AL_NO_ERROR:
                return false;
            case AL10.AL_INVALID_NAME:
                errorMessage( "Invalid name parameter." );
                return true;
            case AL10.AL_INVALID_ENUM:
                errorMessage( "Invalid parameter." );
                return true;
            case AL10.AL_INVALID_VALUE:
                errorMessage( "Invalid enumerated parameter value." );
                return true;
            case AL10.AL_INVALID_OPERATION:
                errorMessage( "Illegal call." );
                return true;
            case AL10.AL_OUT_OF_MEMORY:
                errorMessage( "Unable to allocate memory." );
                return true;
            default:
                errorMessage( "An unrecognized error occurred." );
                return true;
        }
    }
}
