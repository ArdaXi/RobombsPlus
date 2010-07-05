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
 * The SoundSystemLogger class handles all status messages, warnings, and error 
 * messages for the SoundSystem library.  This class can be extended and 
 * methods overriden to change how messages are handled.  To do this, the 
 * overridden class should be instantiated, and a call should be made to method 
 * SoundSystemConfig.setLogger() BEFORE creating the SoundSystem object.  If 
 * the setLogger() method is called after the SoundSystem has been created, 
 * there will be handles floating around to two different message loggers, and 
 * the results will be undesirable.  
 * See {@link paulscode.sound.SoundSystemConfig SoundSystemConfig} for more 
 * information about changing default settings.  If an alternate logger is not 
 * set by the user, then an instance of this base class will be created by 
 * default.
 * 
 * Author: Paul Lamb
 */
public class SoundSystemLogger
{
/**
 * Prints a message.
 * @param message Message to print.
 * @param indent Number of tabs to indent the message.
 */
    public void message( String message, int indent )
    {
        String messageText;
        // Determine how many spaces to indent:
        String spacer = "";
        for( int x = 0; x < indent; x++ )
        {
            spacer += "    ";
        }
        // indent the message:
        messageText = spacer + message;
        
        // Print the message:
        System.out.println( messageText );
    }
    
/**
 * Prints a red message.
 * @param message Message to print.
 * @param indent Number of tabs to indent the message.
 */
    public void importantMessage( String message, int indent )
    {
        String messageText;
        // Determine how many spaces to indent:
        String spacer = "";
        for( int x = 0; x < indent; x++ )
        {
            spacer += "    ";
        }
        // indent the message:
        messageText = spacer + message;
        
        // Print the message:
        System.err.println( messageText );
    }
    
/**
 * Prints the specified message if error is true.
 * @param error True or False.
 * @param classname Name of the class checking for an error.
 * @param message Message to print if error is true.
 * @param indent Number of tabs to indent the message.
 * @return True if error is true.
 */
    public boolean errorCheck( boolean error, String classname, String message, 
                             int indent )
    {
        if( error )
            errorMessage( classname, message, indent );
        return error;
    }
    
/**
 * Prints the classname which generated the error in red, followed by the error 
 * message.
 * @param classname Name of the class which generated the error.
 * @param message The actual error message.
 * @param indent Number of tabs to indent the message.
*/
    public void errorMessage( String classname, String message, int indent )
    {
        String headerLine, messageText;
        // Determine how many spaces to indent:
        String spacer = "";
        for( int x = 0; x < indent; x++ )
        {
            spacer += "    ";
        }
        // indent the header:
        headerLine = spacer + "Error in class '" + classname + "'";
        // indent the message one more than the header:
        messageText = "    " + spacer + message;
        
        // Print the error message:
        System.err.println( headerLine );
        System.out.println( messageText );
    }
    
/**
 * Prints an exception's error message followed by the stack trace.
 * @param e Exception containing the information to print.
 * @param indent Number of tabs to indent the message and stack trace.
 */
    public void printStackTrace( Exception e, int indent )
    {
        printExceptionMessage( e, indent );
        importantMessage( "STACK TRACE:", indent );
        if( e == null )
            return;
        
        StackTraceElement[] stack = e.getStackTrace();
        if( stack == null )
            return;
        
        StackTraceElement line;
        for( int x = 0; x < stack.length; x++ )
        {
            line = stack[x];
            if( line != null )
                message( line.toString(), indent + 1 );
        }
    }
    
/**
 * Prints an exception's error message.
 * @param e Exception containing the message to print.
 * @param indent Number of tabs to indent the message.
 */
    public void printExceptionMessage( Exception e, int indent )
    {
        importantMessage( "ERROR MESSAGE:", indent );
        if( e.getMessage() == null )
            message( "(none)", indent + 1 );
        else
            message( e.getMessage(), indent + 1 );
    }
}
