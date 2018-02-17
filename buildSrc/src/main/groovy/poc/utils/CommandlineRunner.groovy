package poc.utils

//import org.slf4j.Logger
//import org.slf4j.LoggerFactory

/**
 * Invokes things via command-line
 */
class CommandlineRunner {
    //private static Logger logger = LoggerFactory.getLogger(CommandlineRunner.class)

    /**
     * Executes a command and outputs it to a log file
     *
     * @param cmd
     *      The command to run
     * @param outputLog
     *      The log file to output stdout to
     * @return
     *      The exit code returned by the process executed
     */
    static int execute(def cmd, File outputLog) {
        println("Executing command: $cmd")

        if(cmd!=null && cmd.size > 0) {
            ProcessBuilder pb = new ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            Process proc = pb.start()
            
            try {
                PrintStream outlog = new PrintStream(outputLog)
                Scanner is = new Scanner(proc.getInputStream())
                while (is.hasNextLine()) {
                    outlog.println(is.nextLine())
                }
                
                println("Execution complete")
            
            } catch (IOException e) {
                print("Execution ERROR")
                throw new RuntimeException("Error executing command: ${e.toString()}")
            }
            
            return proc.waitFor()
        }
        else {
            throw new Exception("Unexpected EMPTY command")
        }
    }
}
