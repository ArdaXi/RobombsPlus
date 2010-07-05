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
import java.util.LinkedList;

// From the lwjgl library, http://www.lwjgl.org
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * The SourceOpenAL class provides an interface to the lwjgl binding of OpenAL. 
 * For more information about lwjgl, please visit http://www.lwjgl.org 
 * 
 * Author: Paul Lamb
 */
public class SourceOpenAL extends Source
{
/**
 * The source's basic Channel type-cast to a ChannelOpenAL.
 */
    protected ChannelOpenAL channelOpenAL = (ChannelOpenAL) channel;
    
/**
 * OpenAL IntBuffer sound-buffer identifier for this source if it is a normal 
 * source.  
 */
    protected IntBuffer myBuffer;
    
/**
 * FloatBuffer containing the listener's 3D coordinates.  
 */
    protected FloatBuffer listenerPosition;
    
/**
 * FloatBuffer containing the source's 3D coordinates.  
 */
    private FloatBuffer sourcePosition;
    
/**
 * FloatBuffer containing the source's velocity vector.  
 */
    private FloatBuffer sourceVelocity;
    
/**
 * The source's pitch.  
 */
    private float pitch;
    
/**
 * Constructor:  Creates a new source using the specified parameters.
 * @param listPos FloatBuffer containing the listener's 3D coordinates.
 * @param buff OpenAL IntBuffer sound-buffer identifier to use for a new normal source.
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
    public SourceOpenAL( FloatBuffer listPos, IntBuffer buff, boolean pri,
                         boolean strm, boolean lp, String src, String fil,
                         AudioData data, float posX, float posY, float posZ,
                         int att, float dr, boolean tmp )
    {
        super( pri, strm, lp, src, fil, data, posX, posY, posZ, att, dr, tmp );
        listenerPosition = listPos;
        myBuffer = buff;
        libraryType = SoundSystemConfig.LIBRARY_OPENAL;
        resetALInformation();
    }
    
/**
 * Constructor:  Creates a new source matching the specified source.
 * @param listPos FloatBuffer containing the listener's 3D coordinates.
 * @param buff OpenAL IntBuffer sound-buffer identifier to use for a new normal source.
 * @param old Source to copy information from.
 */
    public SourceOpenAL( FloatBuffer listPos, IntBuffer buff, Source old,
                         AudioData data )
    {
        super( old, data );
        listenerPosition = listPos;
        myBuffer = buff;
        libraryType = SoundSystemConfig.LIBRARY_OPENAL;
        resetALInformation();
    }
    
/**
 * Shuts the source down and removes references to all instantiated objects.
 */
    @Override
    public void cleanup()
    {
        
        super.cleanup();
    }
    
/**
 * Changes the peripheral information about the source using the specified 
 * parameters.
 * @param listPos FloatBuffer containing the listener's 3D coordinates.
 * @param buff OpenAL IntBuffer sound-buffer identifier to use for a new normal source.
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
    public void changeSource( FloatBuffer listPos, IntBuffer buff, boolean pri,
                              boolean strm, boolean lp, String src, String fil,
                              AudioData data, float posX, float posY,
                              float posZ, int att, float dr, boolean tmp )
    {
        super.changeSource( pri, strm, lp, src, fil, data, posX, posY, posZ,
                            att, dr, tmp );
        listenerPosition = listPos;
        myBuffer = buff;
        resetALInformation();
    }
    
/**
 * Called every time the listener's position or orientation changes.
 */
    @Override
    public void listenerMoved()
    {
        positionChanged();
    }
    
/**
 * Moves the source to the specified position.
 * @param x X coordinate to move to.
 * @param y Y coordinate to move to.
 * @param z Z coordinate to move to.
 */
    @Override
    public void setPosition( float x, float y, float z )
    {
        super.setPosition( x, y, z );
        
        // Make sure OpenAL information has been created
        if( sourcePosition == null )
            resetALInformation();
        else
            positionChanged();
        
        // put the new position information into the buffer:
        sourcePosition.put( 0, x );
        sourcePosition.put( 1, y );
        sourcePosition.put( 2, z );
        
        // make sure we are assigned to a channel:
        if( channelOpenAL != null && channelOpenAL.ALSource != null )
        {
            // move the source:
            AL10.alSource( channelOpenAL.ALSource.get( 0 ), AL10.AL_POSITION,
                           sourcePosition );
            checkALError();
        }
    }
    
/**
 * Recalculates the distance from the listner and the gain.
 */
    @Override
    public void positionChanged()
    {
        calculateDistance();
        calculateGain();
        
        if( channelOpenAL != null && channelOpenAL.ALSource != null )
        {
            AL10.alSourcef( channelOpenAL.ALSource.get( 0 ),
                            AL10.AL_GAIN, (gain * sourceVolume) );
            checkALError();
        }
    }
    
/**
 * Sets whether this source should loop or only play once.
 * @param lp True or false.
 */
    @Override
    public void setLooping( boolean lp )
    {
        super.setLooping( lp );
        
        // make sure we are assigned to a channel:
        if( channelOpenAL != null && channelOpenAL.ALSource != null )
        {
            if( lp )
                AL10.alSourcei( channelOpenAL.ALSource.get( 0 ),
                                AL10.AL_LOOPING, AL10.AL_TRUE );
            else
                AL10.alSourcei( channelOpenAL.ALSource.get( 0 ),
                                AL10.AL_LOOPING, AL10.AL_FALSE );
            checkALError();
        }
    }
    
/**
 * Sets this source's attenuation model.
 * @param model Attenuation model to use.
 */
    @Override
    public void setAttenuation( int model )
    {
        super.setAttenuation( model );
        // make sure we are assigned to a channel:
        if( channelOpenAL != null && channelOpenAL.ALSource != null )
        {
            // attenuation changed, so update the rolloff factor accordingly
            if( model == SoundSystemConfig.ATTENUATION_ROLLOFF )
                AL10.alSourcef( channelOpenAL.ALSource.get( 0 ),
                                AL10.AL_ROLLOFF_FACTOR, distOrRoll );
            else
                AL10.alSourcef( channelOpenAL.ALSource.get( 0 ),
                                AL10.AL_ROLLOFF_FACTOR, 0.0f );
            checkALError();
        }
    }
    
/**
 * Sets this source's fade distance or rolloff factor, depending on the 
 * attenuation model.
 * @param dr New value for fade distance or rolloff factor.
 */
    @Override
    public void setDistOrRoll( float dr)
    {
        super.setDistOrRoll( dr );
        // make sure we are assigned to a channel:
        if( channelOpenAL != null && channelOpenAL.ALSource != null )
        {
            // if we are using rolloff attenuation, then dr is a rolloff factor:
            if( attModel == SoundSystemConfig.ATTENUATION_ROLLOFF )
                AL10.alSourcef( channelOpenAL.ALSource.get( 0 ),
                                AL10.AL_ROLLOFF_FACTOR, dr );
            else
                AL10.alSourcef( channelOpenAL.ALSource.get( 0 ),
                                AL10.AL_ROLLOFF_FACTOR, 0.0f );
            checkALError();
        }
    }
    
/**
 * Plays the source on the specified channel.
 * @param c Channel to play on.
 */
    @Override
    public void play( Channel c )
    {
        if( !active() )
        {
            if( toLoop )
                toPlay = true;
            return;
        }
        
        if( c == null )
        {
            errorMessage( "Unable to play source, because channel was null" );
            return;
        }
        
        boolean newChannel = (channel != c);
        boolean wasPaused = paused();
        
        super.play( c );
        
        channelOpenAL = (ChannelOpenAL) channel;
        
        // Make sure the channel exists:
        // check if we are already on this channel:
        if( newChannel )
        {
            setPosition( position.x, position.y, position.z );
            
            // Send the source's attributes to the channel:
            if( channelOpenAL != null && channelOpenAL.ALSource != null )
            {
                AL10.alSourcef( channelOpenAL.ALSource.get( 0 ), AL10.AL_PITCH,
                                pitch );
                checkALError();
                AL10.alSource( channelOpenAL.ALSource.get( 0 ),
                               AL10.AL_POSITION, sourcePosition );
                checkALError();
                AL10.alSource( channelOpenAL.ALSource.get( 0 ),
                               AL10.AL_VELOCITY, sourceVelocity );
                checkALError();
                if( attModel == SoundSystemConfig.ATTENUATION_ROLLOFF )
                    AL10.alSourcef( channelOpenAL.ALSource.get( 0 ),
                                    AL10.AL_ROLLOFF_FACTOR, distOrRoll );
                else
                    AL10.alSourcef( channelOpenAL.ALSource.get( 0 ),
                                    AL10.AL_ROLLOFF_FACTOR, 0.0f );
                checkALError();
                if( toLoop && (!toStream) )
                    AL10.alSourcei( channelOpenAL.ALSource.get( 0 ),
                                    AL10.AL_LOOPING, AL10.AL_TRUE );
                else
                    AL10.alSourcei( channelOpenAL.ALSource.get( 0 ),
                                    AL10.AL_LOOPING, AL10.AL_FALSE );
                checkALError();
            }
            if( !toStream )
            {
                // This is not a streaming source, so make sure there is
                // a sound buffer loaded to play:
                if( myBuffer == null )
                {
                    errorMessage( "No sound buffer to play" );
                    return;
                }
                
                channelOpenAL.attachBuffer( myBuffer );
            }
        }
        
        // See if we are already playing:
        if( !playing() )
        {
            if( toStream && !wasPaused )
            {
                if( audioData == null )
                {
                    errorMessage( "Audio Data null in method 'play'" );
                    return;
                }

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
                        errorMessage( "Illegal sample size in method 'play'" );
                        return;
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
                        errorMessage( "Illegal sample size in method 'play'" );
                        return;
                    }
                }
                else
                {
                    errorMessage( "Audio data neither mono nor stereo in " +
                                  "method 'play'" );
                    return;
                }

                // Let the channel know what format and sample rate to use:
                channelOpenAL.setFormat( soundFormat,
                                  (int) audioData.audioFormat.getSampleRate() );
                preLoad = true;
            }
            channel.play();
        }
    }
    
/**
 * Queues up the initial stream-buffers for the stream.
 * @return False if the end of the stream was reached.
 */
    @Override
    public boolean preLoad()
    {
        if( audioData == null || audioData.soundBytes == null
            || audioData.audioFormat == null )
            return false;
        
        LinkedList<byte[]> preLoadBuffers = new LinkedList<byte[]>();
        byte[] data = null;
        
        // rewind
        streamPosition = 0;
        
        int bytesRemaining;
        for( int i = 0; i < SoundSystemConfig.getNumberStreamingBuffers(); i++ )
        {
            bytesRemaining = audioData.soundBytes.array().length
                             - streamPosition;
            if( bytesRemaining <= 0 )
                return false;
            
            if( bytesRemaining < SoundSystemConfig.getStreamingBufferSize() )
                data = new byte[bytesRemaining];
            else
                data = new byte[SoundSystemConfig.getStreamingBufferSize()];
            try
            {
                System.arraycopy( audioData.soundBytes.array(), streamPosition,
                                  data, 0, data.length );
            }
            catch( Exception e )
            {
                data = null;
                break;
            }

            if( data == null )
                break;  // end of stream

            streamPosition += data.length;

            preLoadBuffers.add( data );
        }
        positionChanged();
        
        channel.preLoadBuffers( preLoadBuffers );        
        
        preLoad = false;
        return true;
    }
    
/**
 * Resets all the information OpenAL uses to play this source.
 */
    private void resetALInformation()
    {
        // Create buffers for the source's position and velocity
        sourcePosition = BufferUtils.createFloatBuffer( 3 ).put( 
            new float[] { position.x, position.y, position.z } );        
        sourceVelocity = BufferUtils.createFloatBuffer( 3 ).put( 
            new float[] { 0.0f, 0.0f, 0.0f } );
        
        // flip the buffers, so they can be used:
        sourcePosition.flip();
        sourceVelocity.flip();
        
        // set the source's pitch:
        pitch = 1.0f;
        
        positionChanged();
    }
    
/**
 * Calculates this source's distance from the listener.
 */
    private void calculateDistance()
    {
        if( listenerPosition != null )
        {
            // Calculate the source's distance from the listener:
            double dX = position.x - listenerPosition.get( 0 );
            double dY = position.y - listenerPosition.get( 1 );
            double dZ = position.z - listenerPosition.get( 2 );
            distanceFromListener = (float) Math.sqrt( dX*dX + dY*dY + dZ*dZ );
        }
    }
    
/**
 * If using linear attenuation, calculates the gain for this source based on 
 * its distance from the listener.
 */
    private void calculateGain()
    {
        // If using linear attenuation, calculate the source's gain:
        if( attModel == SoundSystemConfig.ATTENUATION_LINEAR )
        {
            if( distanceFromListener <= 0 )
            {
                gain = 1.0f;
            }
            else if( distanceFromListener >= distOrRoll )
            {
                gain = 0.0f;
            }
            else
            {
                gain = 1.0f - (distanceFromListener / distOrRoll);
            }
            if( gain > 1.0f )
                gain = 1.0f;
            if( gain < 0.0f )
                gain = 0.0f;
        }
        else
        {
            gain = 1.0f;
        }
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
