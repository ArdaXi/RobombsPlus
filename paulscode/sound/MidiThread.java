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

import java.io.IOException;
import java.net.URL;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/**
 * The MidiThread class is used to play back MIDI audio.
 * 
 * Author: Paul Lamb
 */
public class MidiThread extends SimpleThread implements MetaEventListener
{
/**
 * Global identifier for the MIDI "change volume" event.
 */
    private static final int CHANGE_VOLUME = 7;
    
/**
 * Global identifier for the MIDI "end of track" event.
 */
    private static final int END_OF_TRACK = 47;
    
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
    private SoundSystemLogger logger;
    
/**
 * URL to the MIDI file.
 */
    private URL midiSource = null;
    
/**
 * Runs the assigned sequence, passing information on to the synthesizer for 
 * playback.
 */
    private Sequencer sequencer = null;
    
/**
 * Converts MIDI events into audio.
 */
    private Synthesizer synthesizer = null;
    
/**
 * Sequence of MIDI events defining sound.
 */
    private Sequence sequence = null;
    
/**
 * Should playback loop or play only once.
 */
    private boolean toLoop = true;
    
/**
 * True when playback should begin.
 */
    private boolean toPlay = false;
    
/**
 * True when playback should stop.
 */
    private boolean toStop = false;
    
/**
 * True when playback should pause.
 */
    private boolean toPause = false;
    
/**
 * True when a rewind should occur.
 */
    private boolean toRewind = false;
    
/**
 * True when volume should change.
 */
    private boolean toChangeVolume = false;
    
/**
 * True when a new source should be played.
 */
    private boolean newSource = false;
    
/**
 * Playback volume, float value (0.0f - 1.0f).
 */
    private float gain = 1.0f;
    
/**
 * True while sequencer is busy being set up.
 */
    private boolean loading = true;

/**
 * Constructor: Defines MIDI file to play, and whether or not to loop playback.
 * @param file MIDI file to play.
 * @param lp Should file loop or play once?
 */
    public MidiThread( URL file, boolean lp )
    {
        // grab a handle to the message logger:
        logger = SoundSystemConfig.getLogger();
        
        midiSource = file;
        toLoop = lp;
    }
    
/**
 * Ends playback, shuts the thread down and removes references to all 
 * instantiated objects.
 */
    @Override
    public void cleanup()
    {
        kill();
        
        if( sequencer != null )
        {
            try
            {
                sequencer.stop();
                sequencer.close();
                sequencer.removeMetaEventListener( this );
            }
            catch( Exception e )
            {}
        }
        
        midiSource = null;
        logger = null;
        sequencer = null;
        synthesizer = null;
        sequence = null;
        toLoop = true;
        
        super.cleanup();  // Important!
    }
    
/**
 * The main loop for processing commands.  The Command Thread starts out 
 * asleep, and it sleeps again after it finishes processing commands, so it 
 * must be interrupted when commands are queued for processing.
 */
    @Override
    public void run()
    {
        loading = true;
        
        if( midiSource == null )
        {
            errorMessage( "Unable to load Midi file." );
            loading = false;
            return;
        }
        
        try
        {
            sequence = MidiSystem.getSequence( midiSource );
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence( sequence );
        }
        catch( IOException ioe )
        {
            errorMessage( "Input failed while reading from MIDI file." );
            printStackTrace( ioe );
            loading = false;
            return;
        }
        catch( InvalidMidiDataException imde )
        {
            errorMessage( "Invalid MIDI data encountered, or not a MIDI " +
                          "file." );
            printStackTrace( imde );
            loading = false;
            return;
        }
        catch( MidiUnavailableException mue )
        {
            errorMessage( "MIDI unavailable, or MIDI device is already in " +
                          "use." );
            printStackTrace( mue );
            loading = false;
            return;
        }
        catch( Exception e )
        {
            errorMessage( "Problem loading MIDI file." );
            printStackTrace( e );
            loading = false;
            return;
        }
        
        if( !( sequencer instanceof Synthesizer ) )
        {
            try
            {
                synthesizer = MidiSystem.getSynthesizer();
                synthesizer.open();
                Soundbank soundbank = synthesizer.getDefaultSoundbank();
                if( soundbank == null )
                {
                    URL defaultSoundBankFile = getClass().getClassLoader()
                        .getResource(
                                  SoundSystemConfig.getDefaultMidiSoundBank() );
                    Soundbank defaultSoundBank = 
                                MidiSystem.getSoundbank( defaultSoundBankFile );
                    synthesizer.loadAllInstruments( defaultSoundBank );
                }
                
                Receiver receiver = synthesizer.getReceiver();
                Transmitter transmitter = sequencer.getTransmitter();
                transmitter.setReceiver( receiver );
            }
            catch( IOException ioe )
            {
                errorMessage( "Input failed while reading from the default " +
                              "soundbank file." );
                printStackTrace( ioe );
                loading = false;
                return;
            }
            catch( InvalidMidiDataException imde )
            {
                errorMessage( "Invalid MIDI data encountered while reading " +
                              "from the default soundbank file." );
                printStackTrace( imde );
                loading = false;
                return;
            }
            catch( MidiUnavailableException mue )
            {
                errorMessage( "MIDI unavailable, or MIDI device is already " +
                              "in use." );
                printStackTrace( mue );
                loading = false;
                return;
            }
            catch( Exception e )
            {
                errorMessage( "Problem initializing the MIDI sequencer or " +
                              "reading from the default soundbank file." );
                message( "Possible cause:  Unable to locate the default " +
                         "soundbank file.  Ensure that " +
                         "SoundSystemResources.jar is linked at runtime, or " +
                         "that a default sounbank file has been specified." );
                printStackTrace( e );
                loading = false;
                return;
            }
        }
        
        resetGain();
        
        loading = false;
        
        // Start out asleep:
        snooze( 3600000 );
        while( !dying() )
        {
            if( !dying() && toPlay )
            {
                toPlay = false;
                try
                {
                    // start playing:
                    sequencer.start();
                    // event will be sent when end of track is reached:
                    sequencer.addMetaEventListener( this );
                }
                catch( Exception e ){}
            }
            else if( !dying() && toStop )
            {
                toStop = false;
                try
                {
                    // stop playback:
                    sequencer.stop();
                    // rewind to the beginning:
                    sequencer.setMicrosecondPosition( 0 );
                    // No need to listen any more:
                    sequencer.removeMetaEventListener( this );
                }
                catch( Exception e ){}
            }
            else if( !dying() && toPause )
            {
                toPause = false;
                try
                {
                    //stop playback.  Will resume from this location next play.
                    sequencer.stop();
                }
                catch( Exception e ){}
            }
            else if( !dying() && toRewind )
            {
                toRewind = false;
                try
                {
                    // rewind to the beginning:
                    sequencer.setMicrosecondPosition( 0 );
                }
                catch( Exception e ){}
            }
            else if( !dying() && toChangeVolume )
            {
                toChangeVolume = false;
                resetGain();
            }
            else if( !dying() && newSource )
            {
                newSource = false;
                
                loading = true;
                
                try
                {
                    // stop playback:
                    sequencer.stop();
                    // rewind to the beginning:
                    sequencer.setMicrosecondPosition( 0 );
                    // stop looping:
                    sequencer.removeMetaEventListener( this );
                }
                catch( Exception e )
                {}

                if( midiSource == null )
                {
                    errorMessage( "Unable to load Midi file." );
                }
                else
                {
                    try
                    {
                        sequence = MidiSystem.getSequence( midiSource );
                        if( sequencer == null )
                        {
                            sequencer = MidiSystem.getSequencer();
                            sequencer.open();
                            resetGain();
                        }
                        sequencer.setSequence( sequence );
                    }
                    catch( IOException ioe )
                    {
                        System.out.println( "Input failed while reading from " +
                                            "MIDI file." );
                        printStackTrace( ioe );
                        loading = false;
                        return;
                    }
                    catch( InvalidMidiDataException imde )
                    {
                        System.out.println( "Invalid MIDI data encountered, " +
                                            "or not a MIDI file." );
                        printStackTrace( imde );
                        loading = false;
                        return;
                    }
                    catch( MidiUnavailableException mue )
                    {
                        System.out.println( "MIDI unavailable, or MIDI " +
                                            "device is already in use." );
                        printStackTrace( mue );
                        loading = false;
                        return;
                    }
                    catch( Exception e )
                    {
                        System.out.println( "Problem loading MIDI file." );
                        printStackTrace( e );
                        loading = false;
                        return;
                    }
                }
                loading = false;
            }
            
            // Wait until there is something else to do:
            if( !dying() )
                snooze( 3600000 );
        }
        
        cleanup();   // Important!
    }
    
/**
 * Plays the MIDI file from the beginning, or from where it left off if it was 
 * paused.
 */
    public void Play()
    {
        for( int x = 0; loading && x < 500; x++ )
        {
            snooze( 20 );
        }
        
        toPlay = true;
    }

/**
 * Stops playback and rewinds to the beginning.
 */
    public void Stop()
    {
        for( int x = 0; loading && x < 500; x++ )
        {
            snooze( 20 );
        }
        
        toStop = true;
    }
    
/**
 * Temporarily stops playback without rewinding.
 */
    public void Pause()
    {
        for( int x = 0; loading && x < 500; x++ )
        {
            snooze( 20 );
        }
        
        toPause = true;
    }

/**
 * Returns playback to the beginning.
 */
    public void Rewind()
    {
        for( int x = 0; loading && x < 500; x++ )
        {
            snooze( 20 );
        }
        
        toRewind = true;
    }
    
/**
 * Changes the volume of MIDI playback.
 * @param value Float value (0.0f - 1.0f).
 */
    public void setVolume( float value )
    {
        for( int x = 0; loading && x < 500; x++ )
        {
            snooze( 20 );
        }
        
        gain = value;
        toChangeVolume = true;
    }
    
/**
 * Returns the current volume for the MIDI source.
 * @return Float value (0.0f - 1.0f).
 */
    public float getVolume()
    {
        return gain;
    }
    
/**
 * Makes sure playback is running at the correct volume.
 */
    public void checkVolume()
    {
        for( int x = 0; loading && x < 500; x++ )
        {
            snooze( 20 );
        }
        toChangeVolume = true;
    }

/**
 * Redefines MIDI file to play, and whether or not to loop playback.
 * @param file MIDI file to play.
 * @param lp Should file loop or play once?
 */
    public void newSource( URL file, boolean lp )
    {
        for( int x = 0; loading && x < 500; x++ )
        {
            snooze( 20 );
        }
        
        midiSource = file;
        toLoop = lp;
        newSource = true;
    }
/**
 * Sets the value of boolean 'toLoop'.
 * @param value True or False.
 */
    public void setLooping( boolean value )
    {
        toLoop( SET, value );
    }
    
/**
 * Returns the value of boolean 'toLoop'.
 * @return True while looping.
 */
    public boolean getLooping()
    {
        return toLoop( GET, XXX );
    }
    
/**
 * Sets or returns the value of boolean 'toLoop'.
 * @param action GET or SET.
 * @param value New value if action == SET, or XXX if action == GET.
 * @return True while looping.
 */
    private synchronized boolean toLoop( boolean action, boolean value )
    {
        if( action == SET )
            toLoop = value;
        return toLoop;
    }
    
/**
 * Called when MIDI events occur.
 * @param message Meta mssage describing the MIDI event.
 */
    public void meta( MetaMessage message )
    {
        if( message.getType() == END_OF_TRACK )
        {
            // check if we should loop or not:
            if( toLoop )
            {
                // looping
                try
                {
                    // restart from the beginning:
                    sequencer.setMicrosecondPosition( 0 );
                    sequencer.start();
                }
                catch( Exception e ){}
            }
            else
            {
                //non-looping
                try
                {
                    // stop playback:
                    sequencer.stop();
                    // rewind to the beginning:
                    sequencer.setMicrosecondPosition( 0 );
                    // stop looping:
                    sequencer.removeMetaEventListener( this );
                }
                catch( Exception e ){}
            }
        }
    }
    
/**
 * Resets the volume. 
 * 0 = no volume.  1 = maximum volume: initialGain
 * @param g Gain value to use.
 */
    public void resetGain()
    {
        // make sure the value for gain is valid (between 0 and 1)
        if( gain < 0.0f )
            gain = 0.0f;
        if( gain > 1.0f )
            gain = 1.0f;
        
        int midiVolume = (int) ( gain * SoundSystemConfig.getMasterGain()
                                 * 127.0f );
        if( synthesizer != null )
        {
            javax.sound.midi.MidiChannel[] channels = synthesizer.getChannels();
            for( int c = 0; channels != null && c < channels.length; c++ )
            {
                channels[c].controlChange( CHANGE_VOLUME, midiVolume );
            }
        }
        else if( sequencer != null && sequencer instanceof Synthesizer )
        {
            synthesizer = (Synthesizer) sequencer;
            javax.sound.midi.MidiChannel[] channels = synthesizer.getChannels();
            for( int c = 0; channels != null && c < channels.length; c++ )
            {
                channels[c].controlChange( CHANGE_VOLUME, midiVolume );
            }
        }
        else
        {
            try
            {
                Receiver receiver = MidiSystem.getReceiver();
                ShortMessage volumeMessage= new ShortMessage();
                for( int c = 0; c < 16; c++ )
                {
                    volumeMessage.setMessage( ShortMessage.CONTROL_CHANGE, c,
                                              CHANGE_VOLUME, midiVolume );
                    receiver.send( volumeMessage, -1 );
                }
            }
            catch( Exception e )
            {
                errorMessage( "Error resetting gain for MIDI source" );
                printStackTrace( e );
            }
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
        return logger.errorCheck( error, "MidiThread", message, 0 );
    }
    
/**
 * Prints an error message.
 * @param message Message to print.
 */
    private void errorMessage( String message )
    {
        logger.errorMessage( "MidiThread", message, 0 );
    }
    
/**
 * Prints an exception's error message followed by the stack trace.
 * @param e Exception containing the information to print.
 */
    private void printStackTrace( Exception e )
    {
        logger.printStackTrace( e, 1 );
    }
}
