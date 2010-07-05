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

/**
 * The MidiChannel class provides an interface for playing MIDI files, using 
 * the JavaSound API.  For more information about the JavaSound API, visit 
 * http://java.sun.com/products/java-media/sound/ 
 * 
 * Author: Paul Lamb
 */
public class MidiChannel
{
/**
 * Processes status messages, warnings, and error messages.
 */
    private SoundSystemLogger logger;
    
/**
 * Name of the MIDI file.
 */
    private String filename;
    
/**
 * Unique source identifier for this MIDI source.
 */
    private String sourcename;
    
/**
 * Should the MIDI loop or play back only once?
 */
    private boolean toLoop = true;
    
/**
 * Plays back the MIDI from a seperate thread.
 */
    private MidiThread midiThread = null;
    
/**
 * Constructor: Defines the basic source information.
 * @param lp Should playback loop or play only once?
 * @param sname Unique identifier for this source.
 * @param fname Name of the MIDI file to play.
 */
    public MidiChannel( boolean lp, String sname, String fname )
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        URL url;
        // Check if the file is online or inside the JAR:
        if( fname.matches( SoundSystemConfig.PREFIX_URL ) )
        {
            // Online
            try
            {
                url = new URL( fname );
            }
            catch( Exception e )
            {
                errorMessage( "Unable to access online URL" );
                printStackTrace( e );
                cleanup();
                return;
            }
        }
        else
        {
            // Inside the JAR
            url = getClass().getClassLoader().getResource(
                    SoundSystemConfig.getSoundFilesPackage() + fname );
        }
        
        midiThread = new MidiThread( url, toLoop );
        midiThread.start();
        
        filename = fname;
        sourcename = sname;
        toLoop = lp;
    }
    
/**
 * Shuts the channel down and removes references to all instantiated objects.
 */
    public void cleanup()
    {
        if( midiThread != null )
        {
            midiThread.cleanup();
            midiThread.interrupt();
            
            // wait up to 5 seconds for stream thread to end:
            for( int i = 0; i < 50; i++ )
            {
                if( !midiThread.alive() )
                    break;
                try
                {
                    Thread.sleep(100);
                }
                catch(Exception e)
                {}
            }

            if( midiThread.alive() )
            {
                errorMessage( "Midi thread did not die!" );
            }
        }
        
        logger = null;
    }
    
/**
 * Plays the MIDI file from the beginning, or from where it left off if it was 
 * paused.
 */
    public void play()
    {
        if( midiThread != null )
        {
            midiThread.Play();
            midiThread.interrupt();
        }
    }
    
/**
 * Stops playback and rewinds to the beginning.
 */
    public void stop()
    {
        if( midiThread != null )
        {
            midiThread.Stop();
            midiThread.interrupt();
        }
    }
    
/**
 * Temporarily stops playback without rewinding.
 */
    public void pause()
    {
        if( midiThread != null )
        {
            midiThread.Pause();
            midiThread.interrupt();
        }
    }
    
/**
 * Returns playback to the beginning.
 */
    public void rewind()
    {
        if( midiThread != null )
        {
            midiThread.Rewind();
            midiThread.interrupt();
        }
    }
    
/**
 * Changes the volume of MIDI playback.
 * @param value Float value (0.0f - 1.0f).
 */
    public void setVolume( float value )
    {
        if( midiThread != null )
        {
            midiThread.setVolume( value );
            midiThread.interrupt();
        }
    }
    
/**
 * Returns the current volume for the MIDI source.
 * @return Float value (0.0f - 1.0f).
 */
    public float getVolume()
    {
        if( midiThread != null )
            return midiThread.getVolume();
        else
            return 0.0f;
    }
    
/**
 * Makes sure playback is running at the correct volume.
 */
    public void checkVolume()
    {
        if( midiThread != null )
        {
            midiThread.checkVolume();
            midiThread.interrupt();
        }
    }
    
/**
 * Changes the basic information about the MIDI source.
 * @param lp Should playback loop or play only once?
 * @param sname Unique identifier for this source.
 * @param fname Name of the MIDI file to play.
 */
    public void switchSource( boolean lp, String sname, String fname )
    {
        URL url;
        
        // Check if the file is online or inside the JAR:
        if( fname.matches( SoundSystemConfig.PREFIX_URL ) )
        {
            // Online
            try
            {
                url = new URL( fname );
            }
            catch( Exception e )
            {
                errorMessage( "Unable to access online URL" );
                printStackTrace( e );
                cleanup();
                return;
            }
        }
        else
        {
            // Inside the JAR
            url = getClass().getClassLoader().getResource(
                    SoundSystemConfig.getSoundFilesPackage() + fname );
        }
        
        if( midiThread == null || !midiThread.alive() || midiThread.dying() )
            midiThread = new MidiThread( url, lp );
        else
            midiThread.newSource( url, lp );
        
        filename = fname;
        sourcename = sname;
        toLoop = lp;
    }

/**
 * Defines whether playback should loop or only play once.
 * @param value True or False.
 */
    public void setLooping( boolean value )
    {
        toLoop = value;
    }
    
/**
 * Returns whether playback is looping or not.
 * @return True or False.
 */
    public boolean getLooping()
    {
        return toLoop;
    }
    
/**
 * Defines the unique identifier for this source
 * @param value New source name.
 */
    public void setSourcename( String value )
    {
        sourcename = value;
    }
    
/**
 * Returns the unique identifier for this source.
 * @return The source's name.
 */
    public String getSourcename()
    {
        return sourcename;
    }
    
/**
 * Defines which MIDI file to play.  The file may either be located within the 
 * JAR or at an online location.  If the file is online, filename must begin 
 * with "http://", since that is how SoundSystem recognizes URL's.  If the file 
 * is located within the compiled JAR, the package in which sound files are 
 * located may be set by calling SoundSystemConfig.setSoundFilesPackage().
 * @param value Path to the MIDI file.
 */
    public void setFilename( String value )
    {
        filename = value;
    }
    
/**
 * Returns the MIDI file being played.
 * @return Path to the MIDI file.
 */
    public String getFilename()
    {
        return filename;
    }
    
/**
 * Sleeps for the specified number of milliseconds.
 */
    private void snooze( long milliseconds )
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
        return logger.errorCheck( error, "MidiChannel", message, 0 );
    }
    
/**
 * Prints an error message.
 * @param message Message to print.
 */
    protected void errorMessage( String message )
    {
        logger.errorMessage( "MidiChannel", message, 0 );
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

