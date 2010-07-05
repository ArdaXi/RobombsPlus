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

import java.net.URL;    
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

// From the j-ogg library, http://www.j-ogg.de
import de.jarnbjo.ogg.CachedUrlStream;
import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.vorbis.IdentificationHeader;
import de.jarnbjo.vorbis.VorbisStream;


/**
 * The Loader class is used to provide a generic interface for reading from 
 * all audio formats supported by SoundSystem.  Loader can read audio data 
 * either from files that are compiled in the JAR or from files located at an 
 * online location.  Data is read in all at once using the AudioData class.
 * 
 * Author: Paul Lamb
 */
public class Loader
{
/**
 * Processes status messages, warnings, and error messages.
 */
    private SoundSystemLogger logger = null;
    
/**
 * Name of the sound file to load.
 */
    private String filename;
    
/**
 * Location of the sound file to read from.
 */
    private URL myUrl;
    
/**
 * Global identifier for the sound file format.
 */
    private int myFormat = -1;
    
/**
 * Cached URL stream, used for reading .ogg files.
 */
    private CachedUrlStream cachedUrlStream = null;
    
/**
 * Logical Ogg stream, used for reading .ogg files.
 */
    private LogicalOggStream myLogicalOggStream = null;
    
/**
 * Vorbis stream, used for reading .ogg files.
 */
    private VorbisStream myVorbisStream = null;
    
/**
 * Ogg Input stream, used for reading .ogg files.
 */
    private OggInputStream myOggInputStream = null;
    
/**
 * Identification Header, provides information about a .ogg file.
 */
    private IdentificationHeader myIdentificationHeader = null;
    
/**
 * Audio format to use when playing back the wave data.
 */
    private AudioFormat myAudioFormat = null;
    
/**
 * Input stream to use for reading in wave data.
 */
    private AudioInputStream myAudioInputStream = null;
    
/**
 * Constructor: Creates a loader for the specified file.  The file may either 
 * be located within the JAR or at an online location.  If the file is online, 
 * filename must begin with "http://", since that is how SoundSystem recognizes 
 * URL's.  If the file is located within the compiled JAR, the package in which 
 * sound files are located may be set by calling 
 * SoundSystemConfig.setSoundFilesPackage().
 * @param filename Sound file to load.
 */
    public Loader( String filename )
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        this.filename = filename;
    }
    
/**
 * Closes all open streams and removes references to all instantiated objects.
 */
    public void cleanup()
    {
        closeStreams();
        myAudioFormat = null;
        logger = null;
        filename = null;
        myUrl = null;
    }
    
/**
 * Closes all open streams.
 */
    private void closeStreams()
    {
        if( myLogicalOggStream != null )
            try
            {
                myLogicalOggStream.close();
            }
            catch( Exception e )
            {}
        if( myVorbisStream != null )
            try
            {
                myVorbisStream.close();
            }
            catch( Exception e )
            {}
        if( myOggInputStream != null )
            try
            {
                myOggInputStream.close();
            }
            catch( Exception e )
            {}
        if( myAudioInputStream != null )
            try
            {
                myAudioInputStream.close();
            }
            catch( Exception e )
            {}
        myLogicalOggStream = null;
        myVorbisStream = null;
        myOggInputStream = null;
        myAudioInputStream = null;
    }
    
/**
 * Loads the file and returns an AudioData object containing the actual 
 * sound-bytes and the format to use for playback.
 * @param reverseOggByteOrder Whether or not to reverse-order the bytes for .ogg files.
 * @return AudioData information from the file.
 */
    public AudioData load( boolean reverseOggByteOrder )
    {
        AudioData audioData = null;
        
        if( filename.matches( SoundSystemConfig.EXTENSION_WAV ) )
            myFormat = SoundSystemConfig.FORMAT_WAV;
        else if( filename.matches( SoundSystemConfig.EXTENSION_OGG ) )
            myFormat = SoundSystemConfig.FORMAT_OGG;
        else
        {
            myFormat = -1;
            errorMessage( "Unsupported file format: " + filename );
            return null;
        }
        
        // Check if the file is online or inside the JAR:
        if( filename.matches( SoundSystemConfig.PREFIX_URL ) )
        {
            // Online
            try
            {
                myUrl = new URL( filename );
            }
            catch( Exception e )
            {
                errorMessage( "Unable to access online URL" );
                printStackTrace( e );
                cleanup();
                return null;
            }
        }
        else
        {
            // Inside the JAR
            myUrl = getClass().getClassLoader().getResource(
                    SoundSystemConfig.getSoundFilesPackage() + filename );
        }
        
        // Make sure it loaded ok:
        if( errorCheck( (myUrl == null), "Unable to access " + filename ) )
        {
            cleanup();
            return null;
        }
        
        switch( myFormat )
        {
            case SoundSystemConfig.FORMAT_WAV:
                try
                {
                    audioData = AudioData.create( myUrl, false );
                }
                catch( Exception e )
                {
                    errorMessage( "Unable to create WaveData for " + filename );
                    printStackTrace( e );
                    cleanup();
                    return null;
                }
                // Make sure it loaded ok:
                if( errorCheck( ( audioData == null ),
                                "Unable to create WaveData for " + filename ) )
                {
                    cleanup();
                    return null;
                }
                return audioData;
            case SoundSystemConfig.FORMAT_OGG:
                try
                {
                    // Create all the streams:
                    cachedUrlStream = new CachedUrlStream( myUrl );
                    myLogicalOggStream = (LogicalOggStream) 
                          cachedUrlStream.getLogicalStreams().iterator().next();
                    myVorbisStream = new VorbisStream( myLogicalOggStream );
                    myOggInputStream = new OggInputStream( myVorbisStream );

                    // Get the header information about the ogg file:
                    myIdentificationHeader =
                                       myVorbisStream.getIdentificationHeader();
                    
                    // Set up the audio format to use during playback:
                    myAudioFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            (float) myIdentificationHeader.getSampleRate(),
                            16,
                            myIdentificationHeader.getChannels(),
                            myIdentificationHeader.getChannels() * 2,
                            (float) myIdentificationHeader.getSampleRate(),
                            true );
                    
                    // Create the actual audio input stream:
                    myAudioInputStream = new AudioInputStream( myOggInputStream,
                                                            myAudioFormat, -1 );
                }
                catch( Exception e )
                {
                    errorMessage( "Unable to set up input streams for "
                                  + filename );
                    printStackTrace( e );
                    cleanup();
                    return null;
                }
                if( errorCheck( (myAudioInputStream == null),
                                "Unable to set up input streams for "
                                + filename ) )
                {
                    cleanup();
                    return null;
                }
                try
                {
                    audioData = AudioData.create( myAudioInputStream,
                                                  reverseOggByteOrder );
                }
                catch( Exception e )
                {
                    errorMessage( "Exception thrown when creating WaveData for "
                                  + filename );
                    printStackTrace( e );
                    cleanup();
                    return null;
                }
                if( errorCheck( ( audioData == null ),
                                "Unable to create WaveData for " + filename ) )
                {
                    cleanup();
                    return null;
                }
                closeStreams();
                return audioData;
            default:
                errorMessage( "Unsupported format: " + filename );
                cleanup();
                return null;
        }
    }
    
/**
 * Prints a message.
 * @param message Message to print.
 */
    private void message( String message )
    {
        logger.message( message, 0 );
    }
    
/**
 * Prints an important message.
 * @param message Message to print.
 */
    private void importantMessage( String message )
    {
        logger.importantMessage( message, 0 );
    }
    
/**
 * Prints the specified message if error is true.
 * @param error True or False.
 * @param message Message to print if error is true.
 * @return True if error is true.
 */
    private boolean errorCheck( boolean error, String message )
    {
        return logger.errorCheck( error, "Loader", message, 0 );
    }
    
/**
 * Prints an error message.
 * @param message Message to print.
 */
    private void errorMessage( String message )
    {
        logger.errorMessage( "Loader", message, 0 );
    }
    
/**
 * Prints the entire stack trace.
 * @param e Exception containing the stack trace.
 */
    protected void printStackTrace( Exception e )
    {
        logger.printStackTrace( e, 1 );
    }
}
