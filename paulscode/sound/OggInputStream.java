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

import java.io.InputStream;
import java.io.IOException;

// From the j-ogg library, http://www.j-ogg.de
import de.jarnbjo.ogg.EndOfOggStreamException;
import de.jarnbjo.vorbis.VorbisStream;

/**
* The OggInputStream class provides an InputStream interface for reading 
* from a .ogg file.
*/
public class OggInputStream extends InputStream
{    
/**
* The VorbisStream to read from.
*/
    private VorbisStream myVorbisStream;

/**
* Constructor: Use the specified VorbisStream.
* @param source VorbisStream for the ogg file to read from.  
*/
    public OggInputStream( VorbisStream source )
    {
        myVorbisStream = source;
    }

/**
* Implements the InputStream.read() method.  Reads the next byte of data from 
* the input stream.
* @return The next byte of data, or -1 if EOS.  
*/
    public int read() throws IOException
    {
        return 0;
    }

/**
* Overrides the InputStream.read( byte[] ) method.  Reads some number of 
* bytes from the input stream and stores them into the buffer array.
* @param buffer Where to put the read-in data.  
* @return The total number of bytes read into the buffer, or -1 if EOS.
*/
    @Override
    public int read( byte[] buffer ) throws IOException
    {
        return read( buffer, 0, buffer.length );
    }

/**
* Overrides the InputStream.read( byte[], int, int ) method.  Reads up to 
* length bytes of data from the input stream into an array of bytes.
* @param buffer Where to put the read-in data.  
* @param offset Position within buffer to place the data.  
* @param length How much data to read in.  
* @return The total number of bytes read into the buffer, or -1 if EOS.
*/
    @Override
    public int read( byte[] buffer, int offset, int length ) throws IOException
    {
        try
        {
            return myVorbisStream.readPcm( buffer, offset, length );
        }
        catch( EndOfOggStreamException e )
        {
            // no more data left
            return -1;
        }
    }
}
