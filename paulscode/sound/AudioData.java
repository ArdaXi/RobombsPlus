// This is a modified version of the lwjgl WaveData class.  Please read the 
// copyright notice and disclaimer below.
package paulscode.sound;

import java.io.BufferedInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * This is a modified version of the lwjgl WaveData class.
 * For more information about lwjgl, please visit http://www.lwjgl.org
 * 
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are 
 * met:
 * 
 * * Redistributions of source code must retain the above copyright 
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of 
 *   its contributors may be used to endorse or promote products derived 
 *   from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * @author Brian Matzon <brian@matzon.dk>
 * @version $Revision: 2983 $
 * $Id: WaveData.java 2983 2008-04-07 18:36:09Z matzon $
 * 
 * Modified by Paul Lamb, http://www.paulscode.com
 */
public class AudioData
{
/**
 * ByteBuffer containing the sound bytes.
 */
    public ByteBuffer soundBytes;
    
/**
 * Audio format.
 */
    public AudioFormat audioFormat;

/**
 * Creates a new AudioData.
 * 
 * @param soundBytes actual wave data.
 * @param audioFormat format of the wave data.
 */
    private AudioData( ByteBuffer soundBytes, AudioFormat audioFormat )
    {
        this.soundBytes = soundBytes;
        this.audioFormat = audioFormat;
    }

/**
 * Creates an AudioData container from the specified url.
 * 
 * @param path URL to file.
 * @param reverseBytes Whether or not to reverse-order .ogg byte-data.
 * @return AudioData containing data, or null if a failure occured.
 */
    public static AudioData create( URL path, boolean reverseBytes )
    {
        try
        {
            return create( AudioSystem.getAudioInputStream(
                               new BufferedInputStream( path.openStream() ) ), 
                           reverseBytes );
        }
        catch( Exception e )
        {
            errorMessage( "Unable to create from: " + path );
            printStackTrace( e );
            return null;
        }		
    }

/**
 * Creates an AudioData container from the specified stream
 * 
 * @param ais AudioInputStream to read from.
 * @param reverseBytes Whether or not to reverse-order .ogg byte-data.
 * @return AudioData containing data, or null if a failure occured.
 */
    public static AudioData create( AudioInputStream ais,
                                    boolean reverseBytes )
    {
        //get format of data
        AudioFormat audioFormat = ais.getFormat();
        
        if( audioFormat == null )
        {
            errorMessage( "Audio Format null in method 'create'" );
            return null;
        }
        
        byte[] fullBuffer = null;
        
        int fileSize = audioFormat.getChannels() * (int) ais.getFrameLength()
                                        * audioFormat.getSampleSizeInBits() / 8;
        if( fileSize > 0 )
        {
            fullBuffer = new byte[ audioFormat.getChannels()
                                   * (int) ais.getFrameLength()
                                   * audioFormat.getSampleSizeInBits()
                                   / 8 ];
            int read = 0, total = 0;
            try
            {
                while( ( read = ais.read( fullBuffer, total, fullBuffer.length
                                                             - total ) ) != -1
                       && total < fullBuffer.length )
                {
                        total += read;
                }
            }
            catch( Exception e )
            {
                errorMessage( "Exception thrown while reading from the " +
                              "AudioInputStream (location #1)." );
                printStackTrace( e );
                return null;
            }
        }
        else
        {
            int totalBytes, bytesRead, cnt;
            byte[] smallBuffer = null;
            boolean endOfStream = false;

            smallBuffer = new byte[ SoundSystemConfig.getFileChunkSize() ];

            totalBytes = 0;
            while( (!endOfStream) && 
                   (totalBytes < SoundSystemConfig.getMaxFileSize()) )
            {
                bytesRead = 0;
                cnt = 0;
                
                try
                {
                    while( bytesRead < smallBuffer.length )
                    {
                        if( ( cnt = ais.read( smallBuffer, bytesRead,
                                              smallBuffer.length-bytesRead ) )
                             <= 0 )
                        {
                            endOfStream = true;
                            break;
                        }
                        bytesRead += cnt;
                    }
                }
                catch( Exception e )
                {
                    errorMessage( "Exception thrown while reading from the " +
                                  "AudioInputStream (location #2)." );
                    printStackTrace( e );
                    return null;
                }
                
                if( reverseBytes )
                    reverseBytes( smallBuffer, 0, bytesRead );
                totalBytes += bytesRead;
                fullBuffer = appendByteArrays( fullBuffer, smallBuffer,
                                               bytesRead );
            }
        }
        
        //insert data into bytebuffer
        ByteBuffer buffer = convertAudioBytes( fullBuffer,
                                       audioFormat.getSampleSizeInBits() == 16);

        //create our result
        AudioData audiodata =
           new AudioData( buffer, audioFormat );
        
        //close stream
        try
        {
            ais.close();
        }
        catch( Exception e )
        {}

        return audiodata;
    }

/**
 * Converts sound bytes to little-endian format.  
 * @param audio_bytes The original wave data
 * @param two_bytes_data For stereo sounds.
 * @return ByteBuffer containing the converted data.
 */
    private static ByteBuffer convertAudioBytes( byte[] audio_bytes,
                                                 boolean two_bytes_data )
    {
        ByteBuffer dest = ByteBuffer.allocateDirect( audio_bytes.length );
        dest.order( ByteOrder.nativeOrder() );
        ByteBuffer src = ByteBuffer.wrap( audio_bytes );
        src.order( ByteOrder.LITTLE_ENDIAN );
        if( two_bytes_data )
        {
            ShortBuffer dest_short = dest.asShortBuffer();
            ShortBuffer src_short = src.asShortBuffer();
            while( src_short.hasRemaining() )
            {
                dest_short.put(src_short.get());
            }
        }
        else
        {
            while( src.hasRemaining() )
            {
                dest.put( src.get() );
            }
        }
        dest.rewind();
        
        if( !dest.hasArray() )
        {
            byte[] arrayBackedBuffer = new byte[dest.capacity()];
            dest.get( arrayBackedBuffer );
            dest.clear();

            return ByteBuffer.wrap( arrayBackedBuffer );
        }
        
        return dest;
    }
    
/**
 * Creates a new array with the second array appended to the end of the first 
 * array.  
 * @param arrayOne The first array.  
 * @param arrayTwo The second array.  
 * @param length How many bytes to append from the second array.  
 * @return Byte array containing information from both arrays.
 */
    private static byte[] appendByteArrays( byte[] arrayOne, byte[] arrayTwo, 
                                            int length )
    {
        byte[] newArray;
        if( arrayOne == null && arrayTwo == null )
        {
            // no data, just return
            return null;
        }
        else if( arrayOne == null )
        {
            // create the new array, same length as arrayTwo:
            newArray = new byte[ length ];
            // fill the new array with the contents of arrayTwo:
            System.arraycopy( arrayTwo, 0, newArray, 0, length );
            arrayTwo = null;
        }
        else if( arrayTwo == null )
        {
            // create the new array, same length as arrayOne:
            newArray = new byte[ arrayOne.length ];
            // fill the new array with the contents of arrayOne:
            System.arraycopy( arrayOne, 0, newArray, 0, arrayOne.length );
            arrayOne = null;
        }
        else
        {
            // create the new array large enough to hold both arrays:
            newArray = new byte[ arrayOne.length + length ];
            System.arraycopy( arrayOne, 0, newArray, 0, arrayOne.length );
            // fill the new array with the contents of both arrays:
            System.arraycopy( arrayTwo, 0, newArray, arrayOne.length,
                              length );
            arrayOne = null;
            arrayTwo = null;
        }
        
        return newArray;
    }
    
/**
 * Trims the buffer down to a maximum size.
 * @param maxLength Maximum size for the buffer.
 */
    public void trimBuffer( int maxLength )
    {
        if( soundBytes == null || maxLength <= 0 )
            return;
        if( soundBytes.array().length > maxLength )
        {
            byte[] trimmedArray = new byte[maxLength];
            System.arraycopy( soundBytes.array(), 0, trimmedArray, 0,
                              maxLength );
            soundBytes.clear();
            soundBytes = ByteBuffer.wrap( trimmedArray );
        }
    }
    
/**
 * Reverse-orders all bytes in 'buffer'
 * @param buffer Bytes to reverse.
 */
    public static void reverseBytes( byte[] buffer )
    {
        reverseBytes( buffer, 0, buffer.length );
    }    
    
/**
 * Reverse-orders the specified bytes in 'buffer'
 * @param buffer Bytes to reverse.
 * @param offset Where to begin reversing.
 * @param size How many bytes to reverse.
 */
    public static void reverseBytes( byte[] buffer, int offset, int size )
    {

        byte b;
        for( int i = offset; i < ( offset + size ); i += 2 )
        {
            b = buffer[i];
            buffer[i] = buffer[i + 1];
            buffer[i + 1] = b;
        }
    }
    
/**
 * Prints an error message.
 * @param message Message to print.
 */
    private static void errorMessage( String message )
    {
        SoundSystemConfig.getLogger().errorMessage( "AudioData", message, 0 );
    }
    
/**
 * Prints the specified message if error is true.
 * @param error True or False.
 * @param message Message to print if error is true.
 * @return True if error is true.
 */
    private static boolean errorCheck( boolean error, String message )
    {
        return SoundSystemConfig.getLogger().errorCheck( error, "AudioData",
                                                         message, 0 );
    }
    
/**
 * Prints an exception's error message followed by the stack trace.
 * @param e Exception containing the information to print.
 */
    private static void printStackTrace( Exception e )
    {
        SoundSystemConfig.getLogger().printStackTrace( e, 0 );
    }
}
