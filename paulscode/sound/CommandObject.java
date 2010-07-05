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
 * The CommandObject class is used to store arguments in the SoundSystem's
 * Command Queue.  Queued CommandObjects are then processed by the 
 * {@link paulscode.sound.CommandThread CommandThread}.  Commands are queued 
 * and executed in the background, so it is unlikely that the user will ever 
 * need to use this class.  
 * 
 * Author: Paul Lamb
 */
public class CommandObject
{
/**
 * Global identifier for the command to initialize the current sound library.
 */
    public static final int INITIALIZE                  =  1;
/**
 * Global identifier for the command to pre-load a sound file.
 */
    public static final int LOAD_SOUND                  =  2;
/**
 * Global identifier for the command to remove a sound file from memory.
 */
    public static final int UNLOAD_SOUND                =  3;
/**
 * Global identifier for the command to create a new source.
 */
    public static final int NEW_SOURCE                  =  4;
/**
 * Global identifier for the command to create a source and immediately play it.
 */
    public static final int QUICK_PLAY                  =  5;
/**
 * Global identifier for the command to set a source's position in 3D space.
 */
    public static final int SET_POSITION                =  6;
/**
 * Global identifier for the command to change a source's volume.
 */
    public static final int SET_VOLUME                  =  7;
/**
 * Global identifier for the command to change a source's priority.
 */
    public static final int SET_PRIORITY                =  8;
/**
 * Global identifier for the command to tell a source whether or not to loop.
 */
    public static final int SET_LOOPING                 =  9;
/**
 * Global identifier for the command to set a source's attenuation model.
 */
    public static final int SET_ATTENUATION             = 10;
/**
 * Global identifier for the command to set a source's fade distance or rolloff 
 * factor.
 */
    public static final int SET_DIST_OR_ROLL            = 11;
/**
 * Global identifier for the command to set a source's gain.
 */
    public static final int SET_GAIN                    = 12;
/**
 * Global identifier for the command to play a source.
 */
    public static final int PLAY                        = 13;
/**
 * Global identifier for the command to pause a source.
 */
    public static final int PAUSE                       = 14;
/**
 * Global identifier for the command to stop a source.
 */
    public static final int STOP                        = 15;
/**
 * Global identifier for the command to rewind a source.
 */
    public static final int REWIND                      = 16;
/**
 * Global identifier for the command to cull a source.
 */
    public static final int CULL                        = 17;
/**
 * Global identifier for the command to activate a source.
 */
    public static final int ACTIVATE                    = 18;
/**
 * Global identifier for the command to set a source as permanant or temporary.
 */
    public static final int SET_TEMPORARY               = 19;
/**
 * Global identifier for the command to delete a source.
 */
    public static final int REMOVE_SOURCE               = 20;
/**
 * Global identifier for the command to move the listner.
 */
    public static final int MOVE_LISTENER               = 21;
/**
 * Global identifier for the command to set the listener's position.
 */
    public static final int SET_LISTENER_POSITION       = 22;
/**
 * Global identifier for the command to turn the listener.
 */
    public static final int TURN_LISTENER               = 23;
/**
 * Global identifier for the command to set the listener's turn angle.
 */
    public static final int SET_LISTENER_ANGLE          = 24;
/**
 * Global identifier for the command to change the listener's orientation.
 */
    public static final int SET_LISTENER_ORIENTATION    = 25;
/**
 * Global identifier for the command to change the master volume.
 */
    public static final int SET_MASTER_VOLUME           = 26;
    
/**
 * Any int arguments required for a command.
 */
    public int[]          intArgs;
/**
 * Any float arguments required for a command.
 */
    public float[]        floatArgs;
/**
 * Any boolean arguments required for a command.
 */
    public boolean[]      boolArgs;
/**
 * Any String arguments required for a command.
 */
    public String[]       stringArgs;
    
/**
 * Which command to execute.
 */
    public int Command;
    
/**
 * Constructor used to create a command which doesn't require any arguments.
 * @param cmd Which command to execute.
 */
    public CommandObject( int cmd )
    {
        Command = cmd;
    }
/**
 * Constructor used to create a command which requires one integer argument.
 * @param cmd Which command to execute.
 * @param i The integer argument needed to execute this command.
 */
    public CommandObject( int cmd, int i )
    {
        Command = cmd;
        intArgs = new int[1];
        intArgs[0] = i;
    }
/**
 * Constructor used to create a command which requires one float argument.
 * @param cmd Which command to execute.
 * @param f The float argument needed to execute this command.
 */
    public CommandObject( int cmd, float f )
    {
        Command = cmd;
        floatArgs = new float[1];
        floatArgs[0] = f;
    }
/**
 * Constructor used to create a command which requires one String argument.
 * @param cmd Which command to execute.
 * @param s The String argument needed to execute this command.
 */
    public CommandObject( int cmd, String s )
    {
        Command = cmd;
        stringArgs = new String[1];
        stringArgs[0] = s;
    }
/**
 * Constructor used to create a command which requires a String and an int as 
 * arguments.
 * @param cmd Which command to execute.
 * @param s The String argument needed to execute this command.
 * @param i The integer argument needed to execute this command.
 */
    public CommandObject( int cmd, String s, int i )
    {
        Command = cmd;
        intArgs = new int[1];
        stringArgs = new String[1];
        intArgs[0] = i;
        stringArgs[0] = s;
    }
/**
 * Constructor used to create a command which requires a String and a float as 
 * arguments.
 * @param cmd Which command to execute.
 * @param s The String argument needed to execute this command.
 * @param f The float argument needed to execute this command.
 */
    public CommandObject( int cmd, String s, float f )
    {
        Command = cmd;
        floatArgs = new float[1];
        stringArgs = new String[1];
        floatArgs[0] = f;
        stringArgs[0] = s;
    }
/**
 * Constructor used to create a command which requires a String and a boolean 
 * as arguments.
 * @param cmd Which command to execute.
 * @param s The String argument needed to execute this command.
 * @param b The boolean argument needed to execute this command.
 */
    public CommandObject( int cmd, String s, boolean b )
    {
        Command = cmd;
        boolArgs = new boolean[1];
        stringArgs = new String[1];
        boolArgs[0] = b;
        stringArgs[0] = s;
    }
/**
 * Constructor used to create a command which requires three float arguments.
 * @param cmd Which command to execute.
 * @param f1 The first float argument needed to execute this command.
 * @param f2 The second float argument needed to execute this command.
 * @param f3 The third float argument needed to execute this command.
 */
    public CommandObject( int cmd, float f1, float f2, float f3 )
    {
        Command = cmd;
        floatArgs = new float[3];
        floatArgs[0] = f1;
        floatArgs[1] = f2;
        floatArgs[2] = f3;
    }
/**
 * Constructor used to create a command which a String and three float 
 * arguments.
 * @param cmd Which command to execute.
 * @param s The String argument needed to execute this command.
 * @param f1 The first float argument needed to execute this command.
 * @param f2 The second float argument needed to execute this command.
 * @param f3 The third float argument needed to execute this command.
 */
    public CommandObject( int cmd, String s, float f1, float f2, float f3 )
    {
        Command = cmd;
        floatArgs = new float[3];
        stringArgs = new String[1];
        floatArgs[0] = f1;
        floatArgs[1] = f2;
        floatArgs[2] = f3;
        stringArgs[0] = s;
    }
/**
 * Constructor used to create a command which requires six float arguments.
 * @param cmd Which command to execute.
 * @param f1 The first float argument needed to execute this command.
 * @param f2 The second float argument needed to execute this command.
 * @param f3 The third float argument needed to execute this command.
 * @param f4 The fourth float argument needed to execute this command.
 * @param f5 The fifth float argument needed to execute this command.
 * @param f6 The sixth float argument needed to execute this command.
 */
    public CommandObject( int cmd, float f1, float f2, float f3, float f4,
                          float f5, float f6 )
    {
        Command = cmd;
        floatArgs = new float[6];
        floatArgs[0] = f1;
        floatArgs[1] = f2;
        floatArgs[2] = f3;
        floatArgs[3] = f4;
        floatArgs[4] = f5;
        floatArgs[5] = f6;
    }
/**
 * Constructor used to create a command which requires several arguments.
 * @param cmd Which command to execute.
 * @param b1 The first boolean argument needed to execute this command.
 * @param b2 The second boolean argument needed to execute this command.
 * @param b3 The third boolean argument needed to execute this command.
 * @param s1 The first String argument needed to execute this command.
 * @param s2 The second String argument needed to execute this command.
 * @param f1 The first float argument needed to execute this command.
 * @param f2 The second float argument needed to execute this command.
 * @param f3 The third float argument needed to execute this command.
 * @param i The integer argument needed to execute this command.
 * @param f4 The fourth float argument needed to execute this command.
 */
    public CommandObject( int cmd,
                          boolean b1, boolean b2, boolean b3,
                          String s1, String s2,
                          float f1, float f2, float f3,
                          int i, float f4 )
    {
        Command = cmd;
        intArgs = new int[1];
        floatArgs = new float[4];
        boolArgs = new boolean[3];
        stringArgs = new String[2];
        intArgs[0] = i;
        floatArgs[0] = f1;
        floatArgs[1] = f2;
        floatArgs[2] = f3;
        floatArgs[3] = f4;
        boolArgs[0] = b1;
        boolArgs[1] = b2;
        boolArgs[2] = b3;
        stringArgs[0] = s1;
        stringArgs[1] = s2;
    }
/**
 * Constructor used to create a command which requires several arguments.
 * @param cmd Which command to execute.
 * @param b1 The first boolean argument needed to execute this command.
 * @param b2 The second boolean argument needed to execute this command.
 * @param b3 The third boolean argument needed to execute this command.
 * @param s1 The first String argument needed to execute this command.
 * @param s2 The second String argument needed to execute this command.
 * @param f1 The first float argument needed to execute this command.
 * @param f2 The second float argument needed to execute this command.
 * @param f3 The third float argument needed to execute this command.
 * @param i The integer argument needed to execute this command.
 * @param f4 The fourth float argument needed to execute this command.
 * @param b4 The fourth boolean argument needed to execute this command.
 */
    public CommandObject( int cmd,
                          boolean b1, boolean b2, boolean b3,
                          String s1, String s2,
                          float f1, float f2, float f3,
                          int i, float f4,  boolean b4 )
    {
        Command = cmd;
        intArgs = new int[1];
        floatArgs = new float[4];
        boolArgs = new boolean[4];
        stringArgs = new String[2];
        intArgs[0] = i;
        floatArgs[0] = f1;
        floatArgs[1] = f2;
        floatArgs[2] = f3;
        floatArgs[3] = f4;
        boolArgs[0] = b1;
        boolArgs[1] = b2;
        boolArgs[2] = b3;
        boolArgs[3] = b4;
        stringArgs[0] = s1;
        stringArgs[1] = s2;
    }
}
