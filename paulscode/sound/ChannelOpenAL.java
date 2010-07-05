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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

// From the lwjgl library, http://www.lwjgl.org
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * The ChannelOpenAL class is used to reserve a sound-card voice using the 
 * lwjgl binding of OpenAL.  Channels can be either normal or streaming 
 * channels.
 * 
 * For more information about lwjgl, please visit http://www.lwjgl.org 
 * 
 * Author: Paul Lamb
 */
public class ChannelOpenAL extends Channel
{
/**
 * OpenAL's IntBuffer identifier for this channel.
 */    
    IntBuffer ALSource;
    
/**
 * OpenAL data format to use when playing back the assigned source.
 */    
    public int ALformat;	// OpenAL data format
    
/**
 * Sample rate (speed) to use for play-back.
 */    
    public int sampleRate;	// sample rate    
    
/**
 * Constructor:  takes channelType identifier and a handle to the OpenAL 
 * IntBuffer identifier to use for this channel.  Possible values for channel 
 * type can be found in the 
 * {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} class.
 * @param type Type of channel (normal or streaming).
 * @param src Handle to the OpenAL source identifier.
 */
    public ChannelOpenAL( int type, IntBuffer src )
    {
        super( type );
        libraryType = SoundSystemConfig.LIBRARY_OPENAL;
        ALSource = src;
    }
    
/**
 * Empties the streamBuffers list, stops and deletes the ALSource, shuts the 
 * channel down, and removes references to all instantiated objects.
 */
    @Override
    public void cleanup()
    {
        if( ALSource != null )
        {
            try
            {
                // Stop playing the source:
                AL10.alSourceStop( ALSource );
                AL10.alGetError();
            }
            catch( Exception e )
            {}
            try
            {
                // Delete the source:
                AL10.alDeleteSources( ALSource );
                AL10.alGetError();
            }
            catch( Exception e )
            {}
            ALSource.clear();
        }
        ALSource = null;
        
        super.cleanup();
    }
    
/**
 * Attaches an OpenAL sound-buffer identifier for the sound data to be played 
 * back for a normal source.
 * @param buf Intbuffer identifier for the sound data to play.
 * @return False if an error occurred.
 */
    public boolean attachBuffer( IntBuffer buf )
    {
        // A sound buffer can only be attached to a normal source:
        if( errorCheck( channelType != SoundSystemConfig.TYPE_NORMAL,
                        "Sound buffers may only be attached to normal " +
                        "sources." ) )
            return false;
        
        // send the sound buffer to the channel:
        AL10.alSourcei( ALSource.get( 0 ), AL10.AL_BUFFER,
                        buf.get(0) );
        
        // Check for errors and return:
        return checkALError();
    }
    
/**
 * Sets the audio format and sample rate to use when playing back the assigned 
 * source
 * @param format Format to use.
 * @param rate Sample rate (speed) to use.
 */
    public void setFormat( int format, int rate )
    {
        ALformat = format;
        sampleRate = rate;
    }
    
/**
 * Queues up the initial byte[] buffers of data to be streamed.
 * @param bufferList List of the first buffers to be played for a streaming source.
 * @return False if problem occurred or if end of stream was reached.
 */
    @Override
    public boolean preLoadBuffers( LinkedList<byte[]> bufferList )
    {
        // Stream buffers can only be queued for streaming sources:
        if( errorCheck( channelType != SoundSystemConfig.TYPE_STREAMING,
                        "Buffers may only be queued for streaming sources." ) )
            return false;
        
        if( errorCheck( bufferList == null,
                        "Buffer List null in method 'preLoadBuffers'" ) )
            return false;
        
        IntBuffer streamBuffers = BufferUtils.createIntBuffer( 
                                                            bufferList.size() );
        AL10.alGenBuffers( streamBuffers );
        if( errorCheck( checkALError(),
             "Error generating stream buffers in method 'preLoadBuffers'" ) )
            return false;
        
        ByteBuffer byteBuffer = null;
        for( int i = 0; i < bufferList.size(); i++ )
        {
            byteBuffer = ByteBuffer.wrap( bufferList.get(i), 0,
                                          bufferList.get(i).length );
            try
            {
                AL10.alBufferData( streamBuffers.get(i), ALformat, byteBuffer,
                                   sampleRate );
            }
            catch( Exception e )
            {
                errorMessage( "Error creating buffers in method " +
                              "'preLoadBuffers'" );
                printStackTrace( e );
                return false;
            }
            if( errorCheck( checkALError(),
                         "Error creating buffers in method 'preLoadBuffers'" ) )
                return false;
        }
        try
        {
            AL10.alSourceQueueBuffers( ALSource.get( 0 ), streamBuffers );
        }
        catch( Exception e )
        {
            errorMessage( "Error queuing buffers in method 'preLoadBuffers'" );
            printStackTrace( e );
            return false;
        }
        if( errorCheck( checkALError(),
                        "Error queuing buffers in method 'preLoadBuffers'" ) )
            return false;

        AL10.alSourcePlay( ALSource.get( 0 ) );
        if( errorCheck( checkALError(),
                        "Error playing source in method 'preLoadBuffers'" ) )
            return false;
        
        // Success:
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
        
        ByteBuffer byteBuffer = ByteBuffer.wrap( buffer, 0, buffer.length );
        IntBuffer intBuffer = BufferUtils.createIntBuffer( 1 );
        
        AL10.alSourceUnqueueBuffers( ALSource.get( 0 ), intBuffer );
        if( checkALError() )
            return false;
        
        AL10.alBufferData( intBuffer.get(0), ALformat, byteBuffer, sampleRate );
        if( checkALError() )
            return false;
        
        AL10.alSourceQueueBuffers( ALSource.get( 0 ), intBuffer );
        if( checkALError() )
            return false;
        
        return true;
    }
    
/**
 * Returns the number of queued byte[] buffers that have finished playing.
 * @return Number of buffers processed.
 */
    @Override
    public int buffersProcessed()
    {
        // Only streaming sources process buffers:
        if( channelType != SoundSystemConfig.TYPE_STREAMING )
            return 0;
        
        // determine how many have been processed:
        int processed = AL10.alGetSourcei( ALSource.get( 0 ),
                                           AL10.AL_BUFFERS_PROCESSED );
        
        // Check for errors:
        if( checkALError() )
            return 0;
        
        // Return how many were processed:
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
        
        // determine how many have been processed:
        int processed = AL10.alGetSourcei( ALSource.get( 0 ),
                                                    AL10.AL_BUFFERS_QUEUED );
        // Check for errors:
        if( checkALError() )
            return;
        
        IntBuffer intBuffer = BufferUtils.createIntBuffer( 1 );
        while( processed > 0 )
        {
            try
            {
                AL10.alSourceUnqueueBuffers( ALSource.get( 0 ), intBuffer );
            }
            catch( Exception e )
            {
                return;
            }
            if( checkALError() )
                return;
            processed--;
        }
    }
    
/**
 * Stops the channel, dequeues any queued data, and closes the channel.
 */
    @Override
    public void close()
    {
        try
        {
            AL10.alSourceStop( ALSource.get( 0 ) );
            AL10.alGetError();
        }
        catch( Exception e )
        {}
        
        if( channelType == SoundSystemConfig.TYPE_STREAMING )
            flush();
    }
    
/**
 * Plays the currently attached normal source, opens this channel up for 
 * streaming, or resumes playback if this channel was paused.
 */
    @Override
    public void play()
    {
        AL10.alSourcePlay( ALSource.get( 0 ) );
        checkALError();
    }
    
/**
 * Temporarily stops playback for this channel.
 */
    @Override
    public void pause()
    {
        AL10.alSourcePause( ALSource.get( 0 ) );
        checkALError();
    }
    
/**
 * Stops playback for this channel and rewinds the attached source to the 
 * beginning.
 */
    @Override
    public void stop()
    {
        AL10.alSourceStop( ALSource.get( 0 ) );
        checkALError();
    }
    
/**
 * Rewinds the attached source to the beginning.  Stops the source if it was 
 * paused.
 */
    @Override
    public void rewind()
    {
        // rewinding for streaming sources is handled elsewhere
        if( channelType == SoundSystemConfig.TYPE_STREAMING )
            return;
        
        AL10.alSourceRewind( ALSource.get( 0 ) );
        checkALError();        
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
        int state = AL10.alGetSourcei( ALSource.get( 0 ),
                                       AL10.AL_SOURCE_STATE );
        if( checkALError() )
            return false;
        
        return( state == AL10.AL_PLAYING );
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
