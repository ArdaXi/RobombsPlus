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
import javax.sound.sampled.Mixer;

/**
 * The SourceJavaSound class provides an interface to the JavaSound library.  
 * For more information about the Java Sound API, please visit 
 * http://java.sun.com/products/java-media/sound/ 
 * 
 * Author: Paul Lamb
 */
public class SourceJavaSound extends Source
{
/**
 * The source's basic Channel type-cast to a ChannelJavaSound.
 */
    protected ChannelJavaSound channelJavaSound = (ChannelJavaSound) channel;
    
/**
 * Handle to the JavaSound Mixer.
 */
    public Mixer myMixer;
    
/**
 * Handle to the listener information.  
 */
    public ListenerData listener;
    
/**
 * Panning between left and right speaker (float between -1.0 and 1.0).  
 */
    private float pan = 0.0f;
    
/**
 * Constructor:  Creates a new source using the specified parameters.
 * @param mx Handle to the JavaSound Mixer.
 * @param buf Sound buffer to use if creating a new normal source.
 * @param lPos Handle to the listener's position vector.
 * @param lLook Handle to the listener's look-at direction vector.
 * @param lUp Handle to the listener's up-direction vector.
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
    public SourceJavaSound( Mixer mx, ListenerData l, boolean pri, boolean strm,
                            boolean lp, String src, String fil, AudioData data,
                            float x, float y, float z, int att, float dr,
                            boolean tmp )
    {
        super( pri, strm, lp, src, fil, data, x, y, z, att, dr, tmp );
        libraryType = SoundSystemConfig.LIBRARY_JAVASOUND;
        
        myMixer = mx;
        
        // point handle to the listener information:
        listener = l;
        positionChanged();
    }
    
/**
 * Constructor:  Creates a new source matching the specified source.
 * @param mx Handle to the JavaSound Mixer.
 * @param buf Sound buffer to use if creating a new normal source.
 * @param lPos Handle to the listener's position vector.
 * @param lLook Handle to the listener's look-at direction vector.
 * @param lUp Handle to the listener's up-direction vector.
 * @param old Source to copy information from.
 */
    public SourceJavaSound( Mixer mx, ListenerData l, Source old,
                            AudioData data )
    {
        super( old, data );
        libraryType = SoundSystemConfig.LIBRARY_JAVASOUND;

        myMixer = mx;
        
        // point handle to the listener information:
        listener = l;
        positionChanged();
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
 * @param mx Handle to the JavaSound Mixer.
 * @param buf Sound buffer to use if creating a new normal source.
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
    public void changeSource( Mixer mx, boolean pri, boolean strm, boolean lp,
                              String src, String fil, AudioData data, float x,
                              float y, float z, int att, float dr, boolean tmp )
    {
        super.changeSource( pri, strm, lp, src, fil, data, x, y, z, att, dr,
                            tmp );
        myMixer = mx;
        if( channelJavaSound != null )
            channelJavaSound.setLooping( toLoop );
        positionChanged();
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
        positionChanged();
    }
    
/**
 * Updates the pan and gain.
 */
    @Override
    public void positionChanged()
    {
        calculateGain();
        calculatePan();
    }

/**
 * Sets this source's attenuation model.
 * @param model Attenuation model to use.
 */
    @Override
    public void setAttenuation( int model )
    {
        super.setAttenuation( model );
        calculateGain();
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
        calculateGain();
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
        boolean wasStopped = stopped();
        
        super.play( c );
        
        channelJavaSound = (ChannelJavaSound) channel;
        
        // Make sure the channel exists:
        // check if we are already on this channel:
        if( newChannel )
        {
            if( channelJavaSound != null )
                channelJavaSound.setLooping( toLoop );
            
            if( !toStream )
            {
                // This is not a streaming source, so make sure there is
                // a sound buffer loaded to play:
                if( audioData == null )
                {
                    errorMessage( "No sound buffer to play" );
                    return;
                }
                
                channelJavaSound.attachBuffer( audioData );
            }
        }
        positionChanged();  // set new pan and gain
        
        // See if we are already playing:
        if( wasStopped || !playing() )
        {
            if( toStream && !wasPaused )
            {
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
        channelJavaSound.resetStream( audioData.audioFormat );
        positionChanged();  // set new pan and gain
        
        channel.preLoadBuffers( preLoadBuffers );        

        preLoad = false;
        return true;
    }
    
/**
 * Calculates the gain for this source based on its attenuation model and 
 * distance from the listener.
 */
    public void calculateGain()
    {
        float distX = position.x - listener.position.x;
        float distY = position.y - listener.position.y;
        float distZ = position.z - listener.position.z;
        
        distanceFromListener = (float) Math.sqrt( distX*distX + distY*distY
                                                  + distZ*distZ );
        
        // Calculate the source's gain using the specified attenuation model:
        switch( attModel )
        {
            case SoundSystemConfig.ATTENUATION_LINEAR:
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
                    gain = 1.0f - ( distanceFromListener / distOrRoll );
                }
                break;
            case SoundSystemConfig.ATTENUATION_ROLLOFF:
                if( distanceFromListener <= 0 )
                {
                    gain = 1.0f;
                }
                else
                {
                    float tweakFactor = 0.0005f;
                    float attenuationFactor = distOrRoll * distanceFromListener
                                              * distanceFromListener 
                                              * tweakFactor;
                    // Make sure we don't do a division by zero:
                    // (rolloff should NEVER be negative)
                    if( attenuationFactor < 0 )
                        attenuationFactor = 0;
                    
                    gain = 1.0f / ( 1 + attenuationFactor );
                }
                break;
            default:
                gain = 1.0f;
                break;
        }
        // make sure gain is between 0 and 1:
        if( gain > 1.0f )
            gain = 1.0f;
        if( gain < 0.0f )
            gain = 0.0f;
        
        gain *= sourceVolume * SoundSystemConfig.getMasterGain();

        // update the channel's gain:
        if( channelJavaSound != null )
            channelJavaSound.setGain( gain );
    }
    
/**
 * Calculates the panning for this source based on its position in relation to 
 * the listener.
 */
    public void calculatePan()
    {
        Vector3D side = listener.up.cross( listener.lookAt );
        side.normalize();
        float x = position.dot( position.subtract( listener.position ), side );
        float z = position.dot( position.subtract( listener.position ),
                                listener.lookAt );
        side = null;        
        float angle = (float) Math.atan2( x, z );
        pan = (float) - Math.sin( angle );
        
        if( channelJavaSound != null )
            channelJavaSound.setPan( pan );
    }
}
