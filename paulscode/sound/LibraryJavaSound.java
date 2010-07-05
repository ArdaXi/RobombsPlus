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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * The LibraryJavaSound class interfaces the JavaSound library.  
 * For more information about the JavaSound API, visit 
 * http://java.sun.com/products/java-media/sound/ 
 * 
 * Author: Paul Lamb
 */
public class LibraryJavaSound extends Library
{
/**
 * The maximum safe size for a JavaSound clip.
 */
    private final int maxClipSize = 1048576;
    
/**
 * Mixes all the playing sources.
 */
    private static Mixer myMixer = null;
    
/**
 * Constructor: Instantiates the source map, buffer map and listener 
 * information.  Also sets the library type to 
 * SoundSystemConfig.LIBRARY_JAVASOUND
 */
    public LibraryJavaSound() throws SoundSystemException
    {
        super();
        libraryType = SoundSystemConfig.LIBRARY_JAVASOUND;
    }
    
 /**
 * Initializes Javasound.
 */
    @Override
    public void init() throws SoundSystemException
    {
        // No real "loading" for the JavaSound library, just grab the Mixer:
        for( Mixer.Info mixerInfo : AudioSystem.getMixerInfo() )
        {
            if( mixerInfo.getName().equals( "Java Sound Audio Engine" ) )
            {
                // found it!
                myMixer = AudioSystem.getMixer( mixerInfo );
                break;
            }
        }
        
        // Make sure the mixer exists:
        if( myMixer == null )
        {
            importantMessage( "\"Java Sound Audio Engine\" was not found!" );
            super.init();
            throw new SoundSystemException( "\"Java Sound Audio Engine\" was " +
                                         "not found in the list of available " +
                                         "mixers.",
                                         SoundSystemException.JAVASOUND_MIXER );
        }
        
        setMasterVolume( 1.0f );
        
        // Let the user know if everything is ok:
        message( "JavaSound initialized." );
        
        super.init();
    }
    
/**
 * Checks if the JavaSound library type is compatible.
 * @return True or false.
 */
    public static boolean libraryCompatible()
    {
        // No real "loading" for the JavaSound library, just grab the Mixer:
        for( Mixer.Info mixerInfo : AudioSystem.getMixerInfo() )
        {
            if( mixerInfo.getName().equals( "Java Sound Audio Engine" ) )
                return true;
        }
        return false;
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
        return new ChannelJavaSound( type, myMixer );
    }
    
/**
 * Stops all sources, and removes references to all instantiated objects.
 */
    @Override
    public void cleanup()
    {
        super.cleanup();
        myMixer = null;
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
        
        // make sure they gave us a filename:
        if( errorCheck( filename == null,
                              "Filename not specified in method 'loadSound'" ) )
            return false;
        
        // check if it is already loaded:        
        if( bufferMap.get( filename ) != null )
            return true;
        
        Loader loader = new Loader( filename );
        AudioData audioData = loader.load( false );
        loader.cleanup();
        loader = null;
        if( audioData != null )
            bufferMap.put( filename, audioData );
        else
            errorMessage( "Audio Data null in method 'loadSound'" );
        
        return true;
    }
    
 /**
 * Sets the overall volume to the specified value, affecting all sources.
 * @param value New volume, float value ( 0.0f - 1.0f ).
 */ 
    @Override
    public void setMasterVolume( float value )
    {
        super.setMasterVolume( value );
        
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
        
        if( !toStream && audioData != null )
            audioData.trimBuffer( maxClipSize );
        
        sourceMap.put( sourcename,
                       new SourceJavaSound( myMixer, listener, priority, 
                                            toStream, toLoop, sourcename, 
                                            filename, audioData, posX, posY, 
                                            posZ, attModel, distOrRoll,
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
        
        if( !toStream && audioData != null)
            audioData.trimBuffer( maxClipSize );
        
        sourceMap.put( sourcename,
                       new SourceJavaSound( myMixer, listener, priority,
                                            toStream, toLoop, sourcename,
                                            filename, audioData, posX, posY,
                                            posZ, attModel, distOrRoll,
                                            temporary ) );
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
        
        // remove any existing sources before starting:
        sourceMap.clear();
        
        AudioData audioData;
        // loop through and copy all the sources:
        while( iter.hasNext() )
        {
            sourcename = iter.next();
            source = srcMap.get( sourcename );
            if( source != null )
            {
                loadSound( source.filename );
                audioData = bufferMap.get( source.filename );
                if( !source.toStream && audioData != null )
                    audioData.trimBuffer( maxClipSize );
                
                sourceMap.put( sourcename, new SourceJavaSound( myMixer,
                                                listener, source, audioData ) );
            }
        }
    }
    
/**
 * The SoundBuffer class contains a sound file's format and a chunk of byte 
 * data
 */
    public static class SoundBuffer
    {
/**
 * The sound file's format
 */
        public AudioFormat format;
/**
 * A chunk of a sound file's byte data
 */
        public byte[] bytes;
/**
 * Constructor: format and bytes not specified
 */
        public SoundBuffer()
        {
            format = null;
            bytes = null;
        }
/**
 * Constructor: Store the specified format and bytes.
 * @param f The sound file's format.
 * @param b The sound file's byte data.
 */
        public SoundBuffer( AudioFormat f, byte[] b )
        {
            format = f;
            bytes = b;
        }
/**
 * Change format and bytes to null.
 */
        public void cleanup()
        {
            format = null;
            bytes = null;
        }
    }
}
