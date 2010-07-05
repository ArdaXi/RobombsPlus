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

/**
 * The Vector3D class contains methods to simplify common 3D vector functions,
 * such as cross and dot product, normalize, etc.
 * 
 * Author: Paul Lamb
 */
public class Vector3D
{
    
/**
 * The vector's X coordinate.
 */
    public float x;
    
/**
 * The vector's Y coordinate.
 */
    public float y;
    
/**
 * The vector's Z coordinate.
 */
    public float z;

/**
 * Constructor:  Places the vector at the origin.
 */
    public Vector3D()
    {
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }
    
/**
 * Constructor:  Places the vector at the specified 3D coordinates.
 * @param nx X coordinate for the new vector.
 * @param ny Y coordinate for the new vector.
 * @param nz Z coordinate for the new vector.
 */
    public Vector3D( float nx, float ny, float nz )
    {
        x = nx;
        y = ny;
        z = nz;
    }
    
/**
 * Returns a new instance containing the same information as this one.
 * @return A new Vector3D.
 */
    @Override
    public Vector3D clone()
    {
        return new Vector3D( x, y, z );
    }
    
/**
 * Returns a vector containing the cross-product: A cross B.
 * @param A First vector in the cross product.
 * @param B Second vector in the cross product.
 * @return A new Vector3D.
 */
    public Vector3D cross( Vector3D A, Vector3D B )
    {
        return new Vector3D(
                                A.y * B.z - B.y * A.z,
                                A.z * B.x - B.z * A.x,
                                A.x * B.y - B.x * A.y );
    }
    
/**
 * Returns a vector containing the cross-product: (this) cross B.
 * @param B Second vector in the cross product.
 * @return A new Vector3D.
 */
    public Vector3D cross( Vector3D B )
    {
        return new Vector3D(
                                y * B.z - B.y * z,
                                z * B.x - B.z * x,
                                x * B.y - B.x * y );

    }
    
/**
 * Returns the dot-product result of: A dot B.
 * @param A First vector in the dot product.
 * @param B Second vector in the dot product.
 * @return Dot product.
 */
    public float dot( Vector3D A, Vector3D B )
    {
        return( (A.x * B.x) + (A.y * B.y) + (A.z * B.z) );
    }
    
/**
 * Returns the dot-product result of: (this) dot B.
 * @param B Second vector in the dot product.
 * @return Dot product.
 */
    public float dot( Vector3D B )
    {
        return( (x * B.x) + (y * B.y) + (z * B.z) );
    }
    
/**
 * Returns the vector represented by: A + B.
 * @param A First vector.
 * @param B Vector to add to A.
 * @return A new Vector3D.
 */
    public Vector3D add( Vector3D A, Vector3D B )
    {
        return new Vector3D( A.x + B.x, A.y + B.y, A.z + B.z );
    }
    
/**
 * Returns the vector represented by: (this) + B.
 * @param B Vector to add to this one.
 * @return A new Vector3D.
 */
    public Vector3D add( Vector3D B )
    {
        return new Vector3D( x + B.x, y + B.y, z + B.z );
    }
    
/**
 * Returns the vector represented by: A - B.
 * @param A First vector.
 * @param B Vector to subtract from A.
 * @return A new Vector3D.
 */
    public Vector3D subtract( Vector3D A, Vector3D B )
    {
        return new Vector3D( A.x - B.x, A.y - B.y, A.z - B.z );
    }
    
/**
 * Returns the vector represented by: (this) - B.
 * @param B Vector to subtract from this one.
 * @return A new Vector3D.
 */
    public Vector3D subtract( Vector3D B )
    {
        return new Vector3D( x - B.x, y - B.y, z - B.z );
    }
    
/**
 * Changes the length of this vector to 1.0.
 */
    public void normalize()
    {
        double t = Math.sqrt( x*x + y*y + z*z );
        x = (float) (x / t);
        y = (float) (y / t);
        z = (float) (z / t);
    }
}
