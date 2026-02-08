import java.lang.module.Configuration;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A utility class for scanning and loading classes from Java modules.
 */
public class ClassScanner {

    // Private constructor to prevent instantiation from outside
    private ClassScanner() {}

    // Static instance of ClassScanner (singleton instance, created eagerly)
    private static final ClassScanner INSTANCE = new ClassScanner();

    /**
     * Provides access to the singleton instance of ClassScanner.
     * @return the singleton instance of ClassScanner
     */
    public static ClassScanner getInstance() {
        return INSTANCE;
    }

    /**
     * Static method to get the list of all classes found in the modules.
     * Uses the singleton instance to perform the scan.
     * 
     * @return List of classes found across Java modules
     */
    public static List<Class<?>> totalClasses() {
        return INSTANCE.performClassScan();  // Calls the private method that performs the scanning
    }

    /**
     * Performs the actual class scanning by iterating through modules in the boot layer.
     * Only classes within "java." and "jdk." packages are scanned, and only exported packages are included.
     * 
     * @return List of Class&lt;?&gt; objects representing the scanned classes
     */
    private List<Class<?>> performClassScan() {
        List<Class<?>> classes = new ArrayList<>();  // List to store discovered classes
        ModuleLayer bootLayer = ModuleLayer.boot();  // Gets the boot layer, which contains core Java modules
        Configuration bootConfig = bootLayer.configuration();

        // Iterate over modules in the boot layer that start with "java." or "jdk."
        bootLayer.modules().stream()
            .filter(module -> module.getName().startsWith("java.") || module.getName().startsWith("jdk."))
            .forEach(module -> {
                // Resolve each module in the configuration, if available
                Optional<ResolvedModule> resolved = bootConfig.findModule(module.getName());
                resolved.ifPresent(rm -> {
                    ModuleReference ref = rm.reference();
                    // Use ModuleReader to list entries in the module
                    try (ModuleReader reader = ref.open()) {
                        reader.list().forEach(s -> {
                            // Check if entry is a .class file but not a special info file
                            if (s.endsWith(".class") && !(s.equals("package-info.class") || s.equals("module-info.class") || s.equals("META-INF.class"))) {
                                String packageName = s.substring(0, s.lastIndexOf('/')).replace('/', '.');
                                String className = s.replace('/', '.').substring(0, s.length() - ".class".length());
                                // Check if the package is exported from the module
                                if (module.isExported(packageName)) {
                                    try {
                                        // Load the class by its name
                                        Class<?> clazz = Class.forName(className);
                                        if (!classes.contains(clazz)) classes.add(clazz);  // Add if not already in the list
                                        System.out.println("Loaded: " + clazz.getName());
                                    } catch (ClassNotFoundException | SecurityException e) {
                                        System.out.println("Type not found: " + e.getMessage());
                                    }
                                }
                            }
                        });
                    } catch (IOException e) {
                        System.out.println("Could not open Module: " + e.getMessage());
                    }
                });
            });
        return classes;  // Return the list of loaded classes
    }
}
