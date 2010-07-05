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
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import paulscode.sound.LibraryJavaSound.SoundBuffer;

/**
 * The ChannelJavaSound class is used to reserve a sound-card voice using 
 * JavaSound.  Channels can be either normal or streaming channels.
 * 
 * For more information about the JavaSound API, visit 
 * http://java.sun.com/products/java-media/sound/ 
 * 
 * Author: Paul Lamb
 */
public class ChannelJavaSound extends Channel
{
    // NORMAL SOURCE VARRIABLES:
/**
 * Used to play back a normal source.
 */
    public Clip clip = null;
/**
 * The paulscode.sound.LibraryJavaSound.SoundBuffer containing the sound data 
 * to play for a normal source.
 */
    SoundBuffer soundBuffer;
    // END NORMAL SOURCE VARRIABLES
    
    // STREAMING SOURCE VARRIABLES:
/**
 * Used to play back a streaming source.
 */    
    public SourceDataLine sourceDataLine = null;
/**
 * List of paulscode.sound.LibraryJavaSound.SoundBuffer, used to queue chunks 
 * of sound data to be streamed.
 */
    private List<SoundBuffer> streamBuffers;
/**
 * Number of queued stream-buffers that have finished being processed.
 */    
    private int processed = 0;
    
    // END STREAMING SOURCE VARRIABLES:

/**
 * Handle to the Mixer, which is used to mix output from all channels.
 */
    private Mixer myMixer = null;
/**
 * Format to use when playing back the assigned source.
 */
    private AudioFormat myFormat = null;
    
/**
 * Control for changing the gain.
 */
    private FloatControl gainControl = null;
/**
 * Control for changing the pan.
 */
    private FloatControl panControl = null;
        
/**
 * The initial decible change (start at normal volume).
 */
    private float initialGain = 0.0f;
    
/**
 * When toLoop is true, the assigned source is immediately replayed when the 
 * end is reached.
 */
    private boolean toLoop = false;
    
/**
 * Constructor:  takes channelType identifier and a handle to the Mixer as 
 * paramaters.  Possible values for channel type can be found in the 
 * {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} class.
 * @param type Type of channel (normal or streaming).
 * @param mixer Handle to the JavaSound Mixer.
 */
    public ChannelJavaSound( int type, Mixer mixer )
    {
        super( type );
        libraryType = SoundSystemConfig.LIBRARY_JAVASOUND;
        
        myMixer = mixer;
        clip = null;
        sourceDataLine = null;
        streamBuffers = new LinkedList<SoundBuffer>();
    }
    
/**
 * Empties the streamBuffers list, shuts the channel down and removes 
 * references to all instantiated objects.
 */
    @Override
    public void cleanup()
    {
        if( streamBuffers != null )
        {
            SoundBuffer buf = null;
            while( !streamBuffers.isEmpty() )
            {
                buf = streamBuffers.remove( 0 );
                buf.cleanup();
                buf = null;
            }
            streamBuffers.clear();
        }
        
        clip = null;
        soundBuffer = null;
        sourceDataLine = null;
        streamBuffers.clear();
        myMixer = null;
        myFormat = null;
        streamBuffers = null;
        
        super.cleanup();
    }
    
/**
 * Attaches the AudioData to be played back for a normal source.
 * @param AudioData object containing the wave data and format to attach
 * @return False if an error occurred.
 */
    public boolean attachBuffer( AudioData buffer )
    {
        // Can only attach a buffer to a normal source:
        if( errorCheck( channelType != SoundSystemConfig.TYPE_NORMAL,
                        "Buffers may only be attached to non-streaming " +
                        "sources" ) )
            return false;
        
        // make sure the Mixer exists:
        if( errorCheck( myMixer == null,
                        "Mixer null in method 'attachBuffer'" ) )
            return false;
        
        // make sure the buffer exists:
        if( errorCheck( buffer == null,
                        "Buffer null in method 'attachBuffer'" ) )
            return false;
        
        // make sure the buffer exists:
        if( errorCheck( buffer.soundBytes == null,
                        "Buffer missing sound-bytes in method " +
                        "'attachBuffer'" ) )
            return false;
        
        // make sure there is format information about this sound buffer:
        if( errorCheck( buffer.audioFormat == null,
                        "Buffer missing format information in method " +
                        "'attachBuffer'" ) )
            return false;
            
        DataLine.Info lineInfo;
        lineInfo = new DataLine.Info( Clip.class, buffer.audioFormat );
        if( errorCheck( !AudioSystem.isLineSupported( lineInfo ), 
                        "Line not supported in method 'attachBuffer'" ) )
                return false;
        
        Clip newClip = null;
        try
        {
            newClip = (Clip) myMixer.getLine( lineInfo );
        }
        catch( Exception e )
        {
            errorMessage( "Unable to create clip in method 'attachBuffer'" );
            printStackTrace( e );
            return false;
        }
        
        if( errorCheck( newClip == null,
                        "New clip null in method 'attachBuffer'" ) )
            return false;
        
        // if there was already a clip playing on this channel, remove it now:
        if( clip != null )
        {
            clip.stop();
            clip.flush();
            clip.close();
        }
        
        // Update the clip and format varriables:
        clip = newClip;  
        myFormat = buffer.audioFormat;
        newClip = null;
        
        try
        {
            clip.open( myFormat, buffer.soundBytes.array(), 0,
                       buffer.soundBytes.array().length );
        }
        catch( Exception e )
        {
            errorMessage( "Unable to attach buffer to clip in method " +
                          "'attachBuffer'" );
            printStackTrace( e );
            return false;
        }
        
        resetControls();
        
        // Success:
        return true;
    }
    
/**
 * Sets the channel up to be streamed using the specified AudioFormat.
 * @param format Format to use when playing the stream data.
 * @return False if an error occurred.
 */
    public boolean resetStream( AudioFormat format )
    {
        // make sure the Mixer exists:
        if( errorCheck( myMixer == null,
                        "Mixer null in method 'resetStream'" ) )
            return false;
        
        // make sure a format was specified:
        if( errorCheck( format == null,
                        "AudioFormat null in method 'resetStream'" ) )
            return false;
        
        DataLine.Info lineInfo;
        lineInfo = new DataLine.Info( SourceDataLine.class, format );
        if( errorCheck( !AudioSystem.isLineSupported( lineInfo ), 
                        "Line not supported in method 'resetStream'" ) )
            return false;
        
        SourceDataLine newSourceDataLine = null;
        try
        {
            newSourceDataLine = (SourceDataLine) myMixer.getLine( lineInfo );
        }
        catch( Exception e )
        {
            errorMessage( "Unable to create a SourceDataLine " +
                          "in method 'resetStream'" );
            printStackTrace( e );
            return false;
        }
        
        if( errorCheck( newSourceDataLine == null,
                        "New SourceDataLine null in method 'resetStream'" ) )
            return false;
        
        streamBuffers.clear();
        processed = 0;
        
        // if there was already something playing on this channel, remove it:
        if( sourceDataLine != null )
        {
            sourceDataLine.stop();
            sourceDataLine.flush();
            sourceDataLine.close();
        }
        
        // Update the clip and format varriables:
        sourceDataLine = newSourceDataLine;  
        myFormat = format;
        newSourceDataLine = null;
        
        try
        {
            sourceDataLine.open( myFormat );
        }
        catch( Exception e )
        {
            errorMessage( "Unable to open the new SourceDataLine in method " +
                          "'resetStream'" );
            printStackTrace( e );
            return false;
        }
        
        resetControls();
        
        // Success:
        return true;
    }
    
/**
 * (Re)Creates the pan and gain controls for this channel.
 */
    private void resetControls()
    {
        switch( channelType )
        {
            case SoundSystemConfig.TYPE_NORMAL:
                // Check if panning is supported:
                if( errorCheck( !clip.isControlSupported(
                                FloatControl.Type.PAN ), 
                                "Pan control not supported" ) )
                    panControl = null;
                else
                    // Create a new pan Control:
                    panControl = (FloatControl) clip.getControl(
                                                        FloatControl.Type.PAN );
                // Check if changing the volume is supported:
                if( errorCheck( !clip.isControlSupported(
                                FloatControl.Type.MASTER_GAIN ),
                                "Gain control not supported" ) )
                {
                    gainControl = null;
                    initialGain = 0;
                }
                else
                {
                    // Create a new gain control:
                    gainControl = (FloatControl) clip.getControl(
                                                FloatControl.Type.MASTER_GAIN );
                    // Store it's initial gain to use as "maximum volume" later:
                    initialGain = gainControl.getValue();
                }
                break;
            case SoundSystemConfig.TYPE_STREAMING:
                // Check if panning is supported:
                if( errorCheck( !sourceDataLine.isControlSupported(
                                FloatControl.Type.PAN ),
                                "Pan control not supported" ) )
                    panControl = null;
                else
                    // Create a new pan Control:
                    panControl = (FloatControl) sourceDataLine.getControl(
                                                        FloatControl.Type.PAN );
                // Check if changing the volume is supported:
                if( errorCheck( !sourceDataLine.isControlSupported(
                                FloatControl.Type.MASTER_GAIN ),
                                "Gain control not supported" ) )
                {
                    gainControl = null;
                    initialGain = 0;
                }
                else
                {
                    // Create a new gain control:
                    gainControl = (FloatControl) sourceDataLine.getControl(
                                                        FloatControl.Type.MASTER_GAIN );
                    // Store it's initial gain to use as "maximum volume" later:
                    initialGain = gainControl.getValue();
                }
                break;
            default:
                errorMessage( "Unrecognized channel type in method " +
                              "'resetControls'" );
                panControl = null;
                gainControl = null;
                break;
        }
    }
    
/**
 * Defines whether playback should loop or just play once.
 * @param value Loop or not.
 */
    public void setLooping( boolean value )
    {
        toLoop = value;
    }
    
/**
 * Changes the pan between left and right speaker to the specified value. 
 * -1 = left speaker only.  0 = middle, both speakers.  1 = right speaker only. 
 * @param p Pan value to use.
 */
    public void setPan( float p )
    {
        // Make sure there is a pan control
        if( panControl == null )
            return;
        float pan = p;
        // make sure the value is valid (between -1 and 1)
        if( pan < -1.0f )
            pan = -1.0f;
        if( pan > 1.0f )
            pan = 1.0f;
        // Update the pan:
        panControl.setValue( pan );
    }
/**
 * Changes the volume. 
 * 0 = no volume.  1 = maximum volume (initial gain)
 * @param g Gain value to use.
 */
    public void setGain( float g )
    {
        // Make sure there is a gain control
        if( gainControl == null )
            return;
        
        // make sure the value is valid (between 0 and 1)
        float gain = g;
        if( gain < 0.0f )
            gain = 0.0f;
        if( gain > 1.0f )
            gain = 1.0f;
        
        double minimumDB = gainControl.getMinimum();
        double maximumDB = initialGain;

        // convert the supplied linear gain into a "decible change" value
        // minimumDB is no volume
        // maximumDB is maximum volume
        // (Number of decibles is a logrithmic function of linear gain)
        double ampGainDB = ( (10.0f / 20.0f) * maximumDB ) - minimumDB;
        double cste = Math.log(10.0) / 20;
        float valueDB = (float) ( minimumDB + (1 / cste) * Math.log( 1 +
                                                (Math.exp(cste * ampGainDB) - 1)
                                                * gain ) );
        // Update the gain:
        gainControl.setValue( valueDB );
    }
    
/**
 * Queues up the initial byte[] buffers of data to be streamed.
 * @param bufferList List of the first buffers to be played for a streaming source.
 * @return False if problem occurred or end of stream was reached.
 */
    @Override
    public boolean preLoadBuffers( LinkedList<byte[]> bufferList )
    {
        // Stream buffers can only be queued for streaming sources:
        if( errorCheck( channelType != SoundSystemConfig.TYPE_STREAMING,
                        "Buffers may only be queued for streaming sources." ) )
            return false;
        
        // Make sure we have a SourceDataLine:
        if( errorCheck( sourceDataLine == null,
                        "SourceDataLine null in method 'preLoadBuffers'." ) )
            return false;
        
        sourceDataLine.start();

        if( bufferList.isEmpty() )
            return true;

        // preload one stream buffer worth of data:
        byte[] preLoad = bufferList.remove( 0 );
        
        // Make sure we have some data:
        if( errorCheck( preLoad == null,
                        "Missing sound-bytes in method 'preLoadBuffers'." ) )
            return false;
        
        // If we are using more than one stream buffer, pre-load the 
        // remaining ones now:
        while( !bufferList.isEmpty() )
        {
            streamBuffers.add( new SoundBuffer( myFormat,
                                                bufferList.remove( 0 ) ) );
        }
        
        // Pre-load the first stream buffer into the dataline:
        sourceDataLine.write( preLoad, 0, preLoad.length );
        
        processed = 0;
        
        return true;
    }
    
/**
 * Queues up a byte[] buffer of data to be streamed.
 * @param buffer The next buffer to be played for a streaming source.
 * @return False if an error occurred or if the channel is shutting down.
 */
    @Override
    public boolean queueBuffer( byte[] buffer )
    {
        // Stream buffers can only be queued for streaming sources:
        if( errorCheck( channelType != SoundSystemConfig.TYPE_STREAMING,
                        "Buffers may only be queued for streaming sources." ) )
            return false;
        
        // Make sure we have a SourceDataLine:
        if( errorCheck( sourceDataLine == null,
                        "SourceDataLine null in method 'queueBuffer'." ) )
            return false;
        
        // make sure a format was specified:
        if( errorCheck( myFormat == null,
                        "AudioFormat null in method 'queueBuffer'" ) )
            return false;
        
        // Queue a new buffer:
        streamBuffers.add( new SoundBuffer( myFormat, buffer ) );
        
        // Dequeue a buffer and process it:
        processBuffer();
        
        processed = 0;
        return true;
    }
 
/**
 * Plays the next queued byte[] buffer.  This method is run from the seperate 
 * {@link paulscode.sound.StreamThread StreamThread}.
 * @return False when no more buffers are left to process.
 */
    @Override
    public boolean processBuffer()
    {
        // Stream buffers can only be queued for streaming sources:
        if( errorCheck( channelType != SoundSystemConfig.TYPE_STREAMING,
                        "Buffers are only processed for streaming sources." ) )
            return false;
        
        // Make sure we have a SourceDataLine:
        if( errorCheck( sourceDataLine == null,
                        "SourceDataLine null in method 'processBuffer'." ) )
            return false;
        
        if( streamBuffers == null || streamBuffers.isEmpty() )
            return false;
        
        // Dequeue a buffer and feed it to the SourceDataLine:
        SoundBuffer nextBuffer = streamBuffers.remove( 0 );
        sourceDataLine.write( nextBuffer.bytes, 0, nextBuffer.bytes.length );
        
        nextBuffer.cleanup();
        nextBuffer = null;
        
        return true;
    }
    
/**
 * Returns the number of queued byte[] buffers that have finished playing.
 * @return Number of buffers processed.
 */
    @Override
    public int buffersProcessed()
    {
        processed = 0;
        
        // Stream buffers can only be queued for streaming sources:
        if( errorCheck( channelType != SoundSystemConfig.TYPE_STREAMING,
                        "Buffers may only be queued for streaming sources." ) )
        {
            if( streamBuffers != null )
                streamBuffers.clear();
            return 0;
        }
        
        // Make sure we have a SourceDataLine:
        if( errorCheck( sourceDataLine == null,
                        "SourceDataLine null in method 'preLoadBuffers'." ) )
        {
            if( streamBuffers != null )
                streamBuffers.clear();
            return 0;
        }
        
        if( sourceDataLine.available() > 0 )
        {
            processed = 1;
        }
        
        return processed;
    }
    
/**
 * Dequeues all previously queued data.
 */
    @Override
    public void flush()
    {
        // only a streaming source can be flushed:
        // Only streaming sources process buffers:
        if( channelType != SoundSystemConfig.TYPE_STREAMING )
            return;
        
        // Make sure we have a SourceDataLine:
        if( errorCheck( sourceDataLine == null,
                        "SourceDataLine null in method 'flush'." ) )
            return;
        
        sourceDataLine.stop();
        sourceDataLine.flush();
        sourceDataLine.drain();
        
        streamBuffers.clear();
        processed = 0;
    }
    
/**
 * Stops the channel, dequeues any queued data, and closes the channel.
 */
    @Override
    public void close()
    {
        switch( channelType )
        {
            case SoundSystemConfig.TYPE_NORMAL:
                if( clip != null )
                {
                    clip.stop();
                    clip.flush();
                    clip.close();
                }
                break;
            case SoundSystemConfig.TYPE_STREAMING:
                if( sourceDataLine != null )
                {
                    flush();
                    sourceDataLine.close();
                }
                break;
            default:
                break;
        }
    }
    
/**
 * Plays the currently attached normal source, opens this channel up for 
 * streaming, or resumes playback if this channel was paused.
 */
    @Override
    public void play()
    {
        switch( channelType )
        {
            case SoundSystemConfig.TYPE_NORMAL:
                if( clip != null )
                {
                    if( toLoop )
                        clip.loop( Clip.LOOP_CONTINUOUSLY );
                    else
                        clip.start();

                }
                break;
            case SoundSystemConfig.TYPE_STREAMING:
                if( sourceDataLine != null )
                {
                    sourceDataLine.start();
                }
                break;
            default:
                break;
        }
    }
    
/**
 * Temporarily stops playback for this channel.
 */
    @Override
    public void pause()
    {
        switch( channelType )
        {
            case SoundSystemConfig.TYPE_NORMAL:
                if( clip != null )
                    clip.stop();
                break;
            case SoundSystemConfig.TYPE_STREAMING:
                if( sourceDataLine != null )
                    sourceDataLine.stop();
                break;
            default:
                break;
        }
    }
    
/**
 * Stops playback for this channel and rewinds the attached source to the 
 * beginning.
 */
    @Override
    public void stop()
    {
        switch( channelType )
        {
            case SoundSystemConfig.TYPE_NORMAL:
                if( clip != null )
                {
                    clip.stop();
                    clip.setFramePosition(0);
                }
                break;
            case SoundSystemConfig.TYPE_STREAMING:
                if( sourceDataLine != null )
                    sourceDataLine.stop();
                break;
            default:
                break;
        }
    }
    
/**
 * Rewinds the attached source to the beginning.  Stops the source if it was 
 * paused.
 */
    @Override
    public void rewind()
    {
        switch( channelType )
        {
            case SoundSystemConfig.TYPE_NORMAL:
                if( clip != null )
                {
                    boolean rePlay = clip.isRunning();
                    clip.stop();
                    clip.setFramePosition(0);
                    if( rePlay )
                    {
                        if( toLoop )
                            clip.loop( Clip.LOOP_CONTINUOUSLY );
                        else
                            clip.start();
                    }
                }
                break;
            case SoundSystemConfig.TYPE_STREAMING:
                // rewinding for streaming sources is handled elsewhere
                break;
            default:
                break;
        }
    }    
    
/**
 * Used to determine if a channel is actively playing a source.  This method 
 * will return false if the channel is paused or stopped and when no data is 
 * queued to be streamed.
 * @return True if this channel is playing a source.
 */
    @Override
    public boolean playing()
    {
        switch( channelType )
        {
            case SoundSystemConfig.TYPE_NORMAL:
                if( clip == null )
                    return false;
                return clip.isActive();
            case SoundSystemConfig.TYPE_STREAMING:
                if( sourceDataLine == null )
                    return false;
                return sourceDataLine.isActive();
            default:
                return false;
        }
    }
}
