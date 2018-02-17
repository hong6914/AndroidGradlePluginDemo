package poc.utils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.Tasks.*
import org.gradle.api.tasks.TaskInputs
import org.gradle.api.tasks.TaskOutputs
import org.gradle.api.file.FileCollection

import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.api.ApplicationVariantImpl

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import org.apache.commons.io.FileUtils

import java.lang.reflect.Method
import java.lang.reflect.Field


class utilities {
    public static final int PASS_THROUGH_INPUT_NAMES = 1;
    public static final int PASS_THROUGH_INPUT_NAMES_W_PATH = 2;
    public static final int GUARANTEE_UNIQUE_NAMES = 3;

    public static boolean printNames = true;
    public static boolean printCopyPath = true;

    private static Collection<TransformInput> inputs = null;
    private static TransformOutputProvider outputProvider = null;
    private static Context context = null;

    /**
     * debugging utilities
     */

     public static void printTransformInvocation(TransformInvocation transformInvocation, int type)
            throws IOException, TransformException, InterruptedException {
        Map<String, Integer> nameMap = new HashMap<String, Integer>()
        if (printNames || printCopyPath) {
            print("\n\n***********START*************");
        }

        inputs = transformInvocation.getInputs();
        outputProvider = transformInvocation.getOutputProvider();

        if (printNames) {
            print("Type: " + type);
            print("Context Path: " + transformInvocation.getContext().getPath());
            print("isIncremental = " + transformInvocation.isIncremental());
            printTransformInputs(inputs);
        }

        for (TransformInput input: inputs)
        {
            for (DirectoryInput dirInput: input.directoryInputs) {
                File dest = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY);
                if (printCopyPath) {
                    print("Copying " + dirInput.name + " to " + dest.absolutePath)
                }
                FileUtils.copyDirectory(dirInput.getFile(), dest);
            }
            for (JarInput jarInput: input.jarInputs) {
                def src = jarInput.getFile()
                String jarName = jarInput.name;
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length()-4);
                }
                if (type == PASS_THROUGH_INPUT_NAMES_W_PATH) {
                    String path =  src.absolutePath
                    int idx = path.indexOf("exploded-aar")
                    if (idx != -1) {
                        jarName = path.substring(idx+13);
                    }
                } else if (type == GUARANTEE_UNIQUE_NAMES) {
                    if (nameMap.containsKey(jarName)) {
                        int num = nameMap.get(jarName)
                        num++
                        nameMap.put(jarName, num)
                        jarName += "-" + num
                    } else {
                        nameMap.put(jarName, 0);
                    }
                }
                File dest = outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR);
                if (printCopyPath) {
                    print("Copying " + jarName + " to " + dest.absolutePath)
                }
                FileUtils.copyFile(jarInput.getFile(), dest);
            }
        }
        if (printNames || printCopyPath) {
            print("\n************END**************\n");
        }
    }

    private static void print(String s) {
        System.out.println(s);
    }

    private static void printTransformInputs(Collection<TransformInput> inputs) {
        for (TransformInput input: inputs) {
            printTransformInput(input);
        }
    }

    private static void printTransformInput(TransformInput input) {
        print("Inputs :")//+input);
        Collection<DirectoryInput> dirInputs = input.getDirectoryInputs();
        int i=0;
        print("Dir Inputs :")
        for (DirectoryInput dirInput: dirInputs) {
            printQualifiedContent(dirInput, false, i++)
            //print("Directory input["+(i++)+"]: "+ dirInput);
        }
        i=0;
        print("Jar Inputs :")
        Collection<JarInput> jarInputs = input.getJarInputs();
        for (JarInput jarInput: jarInputs) {
            printQualifiedContent(jarInput, true, i++)
            //print("Jar input[" + (i++) + "]: " + jarInput);
        }
    }

    private static void printQualifiedContent(QualifiedContent content, boolean isJar, int num) {
        String line = isJar?"Jar":"Directory"
        line += " " +num
        line += " "+ content.name;
        line += " : " + content.file.absolutePath
        print(line)
    }

    /**
     * dump properties of one object
     */
    public static dumpProperties( String title, def obj, def maxDepth=2 ) {
        println "${title} properties {"
        dumpPropertiesRec( obj, 0, maxDepth )
        println "}"
    }

    public static dumpPropertiesRec( def obj, def indent, def maxDepth=2 ) {
        dumpPropertiesRec_internal( obj, indent, 0, maxDepth )
    }

    public static dumpPropertiesRec_internal( def obj, def indent, def currentDepth, def maxDepth ) {
        if( maxDepth != 0 && maxDepth == currentDepth ) {
            for( i in 0 .. indent )
                print "    "
            println "<max-depth reached>"
            return
        }

        if( obj != null && obj.hasProperty("metaClass") && !(obj.metaClass == null) ) {
            for( i in 0 .. indent )
                print "    "
            println "Methods {"
            for( i in 0 .. (indent + 1) )
                print "    "
            println obj.metaClass.methods*.name.sort().unique()
            for( i in 0 .. indent )
                print "    "
            println "}"
        }

        obj.properties.each { k, v ->
            for( i in 0 .. indent )
                print "    "
            print "$k : $v"
            if( !k.equals("asDynamicObject") &&
                v != null && v instanceof Object &&
                !(v instanceof Class) &&
                !(v instanceof java.lang.Long) &&
                !(v instanceof java.lang.Integer) &&
                !(v instanceof java.lang.Short) &&
                !(v instanceof java.lang.Byte) &&
                !(v instanceof java.lang.Character) &&
                !(v instanceof java.lang.Boolean) &&
                !(v instanceof java.lang.String) &&
                !(v instanceof java.lang.Double) &&
                !(v instanceof java.lang.Float))
            {
                if( v == null )
                    throw new RuntimeException("WTF?")
                if( v.getClass() != null ) {
                    /*
                    if( v.getClass().toString() == null )
                        throw new RuntimeException("Again, WTF?")
                        */
                    println " (${v.getClass()}) {"
                }
                else {
                    println " {"
                }
                dumpPropertiesRec_internal( v, indent + 1, currentDepth + 1, maxDepth )
                for( i in 0 .. indent )
                    print "    "
                println "}"
            }
            else
            {
                println ""
            }
        }
    }

    /**
     * Print all Inputs and Outputs of all the tasks, per project in the build variant
     * @param tasks
     *      Task set per project in the build variant
     */
    public static void printTasksIO(Set<Task> tasks, boolean printTaskDetails = true) {
        if (tasks == null || tasks.size() == 0)
            return;

        println("\nDump task inputs/outputs\n");
        for (Task t: tasks) {
            printTaskIO(t, printTaskDetails);
        }
    }

    /**
     * Print all Inputs and Outputs of one task
     * @param oneTask
     */
    public static void printTaskIO(Task oneTask, boolean printTaskDetails = true) {
        if (oneTask == null)
            return;

        println("\n-----Task = " + oneTask.name + "----------------------------------------------");

        if (printTaskDetails) {
            println "\t----- properties: begin"
            println oneTask.getProperties().collect{it}.join('\n')
            println "\t----- properties: end"

            if (oneTask.getInputs().hasInputs) {
                println("\t*****Inputs*****");
                println oneTask.getInputs().getFiles().collect{it}.join('\n')
            }

            if (oneTask.getOutputs().hasOutput) {
                println("\t*****Outputs*****");
                println oneTask.getOutputs().getFiles().collect{it}.join('\n')
            }
        }
    }

}
