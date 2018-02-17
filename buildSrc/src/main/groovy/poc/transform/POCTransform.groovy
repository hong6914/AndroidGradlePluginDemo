package poc.transform

import org.gradle.api.Project

import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import org.apache.commons.io.FileUtils
import java.util.zip.ZipFile

import poc.utils.FileMethods
import poc.utils.Versions


public class POCTransform extends Transform {

    private Map<String, ApplicationVariantImpl> buildVariantMap = null;
    private Project parentProject;
    private QualifiedContent.DefaultContentType dataType;
    private boolean printNames, printCopyPath;

    private static Collection<TransformInput> inputs = null;
    private static TransformOutputProvider outputProvider = null;
    private static Context context = null;

    public POCTransform(Project parentProject, Map<String, ApplicationVariantImpl> variantsMap, boolean doClass) {
        this.buildVariantMap = variantsMap;
        this.parentProject = parentProject;
        if (doClass == true) {
            this.dataType = QualifiedContent.DefaultContentType.CLASSES;
        } else {
            this.dataType = QualifiedContent.DefaultContentType.RESOURCES;
        }

        println("\n=====REGISTERED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
    }
    
    @Override
    public String getName() {
        return "POC_" + (this.dataType == QualifiedContent.DefaultContentType.CLASSES? "Class" : "Resource") + "Transformation";
    }

    @Override
    public Set<ContentType> getInputTypes() {
        return ImmutableSet.<ContentType>of(this.dataType);
    }
    
    @Override
    public Set<Scope> getScopes() {
        if (Versions.isPluginVersion3orAbove()) {
            // Google takes away Scope.<PROJECT_LOCAL_DEPS, SUB_PROJECTS_LOCAL_DEPS> from Android Gradle Plugin v3.0.
            return Sets.immutableEnumSet(Scope.PROJECT, Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES);
        } else {
            return Sets.immutableEnumSet(Scope.PROJECT, Scope.PROJECT_LOCAL_DEPS, Scope.SUB_PROJECTS, Scope.SUB_PROJECTS_LOCAL_DEPS, Scope.EXTERNAL_LIBRARIES);
        }
    }

    // We do not support incremental build at the moment.
    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws IOException, TransformException, InterruptedException {
        inputs = transformInvocation.getInputs();
        outputProvider = transformInvocation.getOutputProvider();
        context = transformInvocation.getContext();

        if (this.dataType == QualifiedContent.DefaultContentType.CLASSES) {
            this.classTransform(transformInvocation);
        } else {
            this.resourceTransform(transformInvocation);
        }
    }

    private void classTransform(TransformInvocation transformInvocation)
        throws IOException, TransformException, InterruptedException {
        Map<File, File> inputOutputPairsForGS = new LinkedHashMap<>();
        Map<File, File> transformedInputOutputPairs = new LinkedHashMap<>();
        File transformedRootDir = new File(context.getTemporaryDir(), "transformedclasses");
        //println( "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ---------------------------------");
        //println parentProject.getProperties().collect{it}.join('\n');

        //println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ================================" + context.getVariantName());
        //println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ================================" + this.project.android.compileSdkVersion);

        inputs.each {
            it.getDirectoryInputs().each {
                //println( "\n-----DIR = " + it.file.absolutePath + "\t" + it.getScopes().toString() + "\t" + it.getContentTypes());
                final File input = it.file
                final File output = outputProvider.getContentLocation(it.name, this.getInputTypes(), this.getScopes(), Format.DIRECTORY);

                if (it.getScopes().contains(Scope.PROJECT) || it.getScopes().contains(Scope.SUB_PROJECTS)) {
                    File transformedOne = new File(transformedRootDir, input.getName());
                    inputOutputPairsForGS.put(input, transformedOne);
                    transformedInputOutputPairs.put(transformedOne, output);
                } else {
                    // directly copy it for the next transformation
                    FileMethods.copyDirectoryTo(input, output, true, true);
                }
            }

            it.getJarInputs().each {
                //println( "\n-----JAR = " + it.file.absolutePath + "\t" + it.getScopes().toString() + "\t" + it.getContentTypes());
                final File input = it.file
                final File output = outputProvider.getContentLocation(it.name, this.getInputTypes(), this.getScopes(), Format.JAR);

                if (it.getScopes().contains(Scope.PROJECT_LOCAL_DEPS) || it.getScopes().contains(Scope.SUB_PROJECTS_LOCAL_DEPS)) {
                    File transformedOne = new File(transformedRootDir, input.getName());
                    inputOutputPairsForGS.put(input, transformedOne);
                    transformedInputOutputPairs.put(transformedOne, output);
                } else {
                    // directly copy it for the next transformation
                    FileMethods.copyTo(input, output, true, true);
                }
            }
        }

        // collect referenced class paths
        ArrayList classPath = new ArrayList();
        String buildVariantName = context.getVariantName()
        ApplicationVariantImpl currentVariant = buildVariantMap[buildVariantName]
        if (currentVariant == null) {
            throw new RuntimeException("BUGBUG: the POC plugin requires build variant: " + buildVariantName);
        } else {
            // The v3.0 version does not extract embedded JAR files from the AAR libraries, so we have to DIY to get them out.
            if (Versions.isPluginVersion3orAbove()) {
                currentVariant.compileConfiguration.files.each() { one->
                    if (one.name.endsWith('.aar')) {
                        classPath.add( ExtractJarFromLibrary(one.canonicalPath) );
                    } else {
                        classPath.add(one);
                    }
                }
            } else {
                currentVariant.compileLibraries.each() { classPath.add(it); }
            }
        }

        String androidLib = System.getenv("ANDROID_HOME") + File.separator + "platforms/${parentProject.android.compileSdkVersion}/android.jar";
        classPath.add(androidLib);

        ArrayList<String> extDirs = new ArrayList<String>(Arrays.asList(androidLib));

        // FAKED POC transformations

        inputOutputPairsForGS.each { k, v ->
            if (k.isDirectory()) {
                FileMethods.copyDirectoryTo(k, v, true, true);
            } else {
                FileMethods.copyTo(k, v, true, true);
            }
        }

        // Continue the transformation for the next task
        transformedInputOutputPairs.each { k, v ->
            if (k.isDirectory()) {
                FileMethods.copyDirectoryTo(k, v, true);
            } else {
                FileMethods.copyTo(k, v, true);
            }
        }
    }


    /**
     * Extract embedded <classes.jar> from one AAR library to a temp file,
     * which will be deleted when the process exits.
     */
    private String ExtractJarFromLibrary(String libraryFileName) {
        if (libraryFileName.isEmpty())
            return null;
        def jarFile = null
        def zipFile = new ZipFile(libraryFileName)

        zipFile.entries().each { ins->
            if (! ins.directory && ins.name.equals('classes.jar')) {
                def tempFile = File.createTempFile('temp', '.jar')
                tempFile.deleteOnExit()
                tempFile.withOutputStream { os ->
                    os << zipFile.getInputStream(ins)
                }
                jarFile = tempFile.canonicalPath
            }
        }

        return jarFile
    }


    private void resourceTransform(TransformInvocation transformInvocation)
            throws IOException, TransformException, InterruptedException {
        Map<File, File> inputOutputPairsForGS = new LinkedHashMap<>();
        Map<File, File> transformedInputOutputPairs = new LinkedHashMap<>();
        File transformedRootDir = new File(context.getTemporaryDir(), "transformedresources");

        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ============================= JAVA RESOURCE TRANSFORMATION");
        println("inputs=" + inputs);

        inputs.each {
            it.getDirectoryInputs().each {
                print( "-----DIR = " + it.file.absolutePath + "\t" + it.getScopes().toString());
                final File input = it.file
                final File output = outputProvider.getContentLocation(it.name, this.getInputTypes(), this.getScopes(), Format.DIRECTORY);

                if (it.getScopes().contains(Scope.PROJECT) || it.getScopes().contains(Scope.SUB_PROJECTS)) {
                    File transformedOne = new File(transformedRootDir, input.getName());
                    inputOutputPairsForGS.put(input, transformedOne);
                    transformedInputOutputPairs.put(transformedOne, output);
                } else {
                    // directly copy it for the next transformation
                    FileMethods.copyDirectoryTo(input, output, true, true);
                }
            }

            it.getJarInputs().each {
                print( "-----JAR = " + it.file.absolutePath + "\t" + it.getScopes().toString() );
                final File input = it.file
                final File output = outputProvider.getContentLocation(it.name, this.getInputTypes(), this.getScopes(), Format.JAR);

                if (it.getScopes().contains(Scope.PROJECT_LOCAL_DEPS) || it.getScopes().contains(Scope.SUB_PROJECTS_LOCAL_DEPS)) {
                    File transformedOne = new File(transformedRootDir, input.getName());
                    inputOutputPairsForGS.put(input, transformedOne);
                    transformedInputOutputPairs.put(transformedOne, output);
                } else {
                    // directly copy it for the next transformation
                    FileMethods.copyTo(input, output, true, true);
                }
            }

            // FAKED POC Java resource transformations --- we don't deal with it here.

            inputOutputPairsForGS.each { k, v ->
                if (k.isDirectory()) {
                    FileMethods.copyDirectoryTo(k, v, true, true);
                } else {
                    FileMethods.copyTo(k, v, true, true);
                }
            }

            // continue the transformation for the next task
            transformedInputOutputPairs.each { k, v ->
                if (k.isDirectory()) {
                    FileMethods.copyDirectoryTo(k, v, true);
                } else {
                    FileMethods.copyTo(k, v, true);
                }
            }
        }

    }
}