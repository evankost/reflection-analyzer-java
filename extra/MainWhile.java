import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class MainWhile {

    // Path for the output file and the number of top entries to include
    private static String outputFilePath;
    private static int topN;
    private static List<Class<?>> classes;

    // Maps to store counts of fields, methods, subtypes, and supertypes for each class
    private static final Map<String, Integer> fieldsDeclared = new HashMap<>(), 
                                              fieldsAll = new HashMap<>(), 
                                              methodsAll = new HashMap<>(), 
                                              methodsDeclared = new HashMap<>(), 
                                              subtypesTotal = new HashMap<>(), 
                                              supertypesTotal = new HashMap<>();
      
    public static void main(String[] args) {

        // Handle command-line arguments to set file paths and topN limit
        switch (args.length) {
            case 1 -> {
                // Case with 1 argument: Set topN, default output file path, and load all classes
                topN = Integer.parseInt(args[0]);
                outputFilePath = "output.txt";
                classes = ClassScanner.totalClasses();
            }
            case 3 -> {
                // Case with 3 arguments: Set input and output file paths and topN, then load classes
                String inputFilePath = args[0];
                outputFilePath = args[1];
                topN = Integer.parseInt(args[2]);
                classes = inputClasses(new ArrayList<>(), inputFilePath);
            }
            default -> {
                // Invalid usage; print instructions and exit
                System.out.println("Invalid arguments. Usage:");
                System.out.println("1 argument: java Main <value-of-N>");
                System.out.println("3 arguments: java Main <input-file> <output-file> <value-of-N>");
                return;
            }
        }

        // If classes were successfully loaded, proceed with analysis
        if (!classes.isEmpty()) {
            System.out.println("Found " + classes.size() + " Classes");
            classes.forEach(MainWhile::exploreHierarchy);  // Analyze each class's hierarchy

            // Prepare output lines with results for fields, methods, subtypes, and supertypes
            List<String> outputLines = new ArrayList<>();
            outputLines.add("1a: " + sortMapByValueToString(fieldsDeclared, topN));
            outputLines.add("1b: " + sortMapByValueToString(fieldsAll, topN));
            outputLines.add("2a: " + sortMapByValueToString(methodsDeclared, topN));
            outputLines.add("2b: " + sortMapByValueToString(methodsAll, topN));
            outputLines.add("3: " + sortMapByValueToString(subtypesTotal, topN));
            outputLines.add("4: " + sortMapByValueToString(supertypesTotal, topN));

            // Write results to the specified output file
            writeFile(outputFilePath, outputLines);
        }
    }

    /**
     * Reads class names from the specified input file, loads each class, and adds it to the classes list.
     * @param classes List to hold the loaded classes
     * @param inputFilePath Path to the file containing class names
     * @return List of loaded classes
     */
    private static List<Class<?>> inputClasses(List<Class<?>> classes, String inputFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            String typeName;
            while ((typeName = br.readLine()) != null) {

                // Skip entries that are not valid class names
                if (typeName.endsWith("package-info") || typeName.endsWith("module-info") || typeName.contains("META-INF")) {
                    System.out.println("Skipped non-class entry: " + typeName);
                    continue;
                }

                try {
                    // Attempt to load the class by its name
                    Class<?> clazz = Class.forName(typeName);
                    classes.add(clazz);
                    System.out.println("Loaded: " + clazz.getName());
                } catch (ClassNotFoundException e) {
                    System.out.println("Type not found: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
        }

        return classes;
    }

    /**
     * Analyzes each class by counting its declared fields, methods, subtypes, and supertypes.
     * @param clazz The class to be analyzed
     */
    private static void exploreHierarchy(Class<?> clazz) {

        // Sets to track unique field names, method names, and supertypes for each class
        Set<String> uniqueFieldNames = new HashSet<>();
        Set<String> uniqueMethodNames = new HashSet<>();
        Set<String> supertypes = new HashSet<>();

        // Count declared fields and methods (not including inherited members)
        for (Field field : clazz.getDeclaredFields()) uniqueFieldNames.add(field.getName());
        for (Method method : clazz.getDeclaredMethods()) uniqueMethodNames.add(method.getName());

        // Store counts for declared fields and methods
        fieldsDeclared.put(clazz.getName(), uniqueFieldNames.size());
        methodsDeclared.put(clazz.getName(), uniqueMethodNames.size());

        // Explore superclass and interfaces
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null) {
            // Add non-private fields and methods from the superclass
            for (Field field : superclass.getDeclaredFields()) {
                if (!Modifier.isPrivate(field.getModifiers())) uniqueFieldNames.add(field.getName());
            }
            for (Method method : superclass.getDeclaredMethods()) {
                if (!Modifier.isPrivate(method.getModifiers())) uniqueMethodNames.add(method.getName());
            }

            // Explore interfaces and update the superclass reference for further traversal
            exploreInterfaces(superclass, uniqueFieldNames, uniqueMethodNames);
            superclass = superclass.getSuperclass();
        }

        // Explore interfaces implemented by the original class
        exploreInterfaces(clazz, uniqueFieldNames, uniqueMethodNames);
        countSubAndSupertypes(clazz, subtypesTotal, supertypes);  // Count subtypes and supertypes

        // Store counts for all (declared + inherited) fields and methods, and supertypes
        fieldsAll.put(clazz.getName(), uniqueFieldNames.size());
        methodsAll.put(clazz.getName(), uniqueMethodNames.size());
        supertypesTotal.put(clazz.getName(), supertypes.size());
    }

    /**
     * Recursively explores interfaces of a class, adding inherited fields and methods to the sets.
     * @param clazz The current class or interface in the hierarchy
     * @param uniqueFieldNames Set of unique field names to track inherited fields
     * @param uniqueMethodNames Set of unique method names to track inherited methods
     */
    private static void exploreInterfaces(Class<?> clazz, Set<String> uniqueFieldNames, Set<String> uniqueMethodNames) {
        for (Class<?> iface : clazz.getInterfaces()) {

            // Add all fields and non-private methods from the interface
            for (Field field : iface.getDeclaredFields()) uniqueFieldNames.add(field.getName());
            for (Method method : iface.getDeclaredMethods()) {
                if (!Modifier.isPrivate(method.getModifiers())) uniqueMethodNames.add(method.getName());
            }

            // Recursively explore extended interfaces
            exploreInterfaces(iface, uniqueFieldNames, uniqueMethodNames);
        }
    }

    /**
     * Counts the number of subtypes and supertypes in the hierarchy for a class.
     * @param clazz The current class in the hierarchy
     * @param subtypeCounts Map to store counts of subtypes for each class
     * @param supertypes Set of supertypes for the current class
     */
    private static void countSubAndSupertypes(Class<?> clazz, Map<String, Integer> subtypeCounts, Set<String> supertypes) {

        // Count superclass, if it exists
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            subtypeCounts.put(superclass.getName(), subtypeCounts.getOrDefault(superclass.getName(), 0) + 1);
            supertypes.add(superclass.getName());
            countSubAndSupertypes(superclass, subtypeCounts, supertypes);  // Recursive call for superclass
        }

        // Count implemented interfaces
        for (Class<?> superInterface : clazz.getInterfaces()) {
            subtypeCounts.put(superInterface.getName(), subtypeCounts.getOrDefault(superInterface.getName(), 0) + 1);
            supertypes.add(superInterface.getName());
            countSubAndSupertypes(superInterface, subtypeCounts, supertypes);  // Recursive call for interfaces
        }
    }

    /**
     * Writes the formatted output data to the specified output file.
     * @param outputFilePath Path to the output file
     * @param outputLines List of strings representing the formatted output data
     */
    private static void writeFile(String outputFilePath, List<String> outputLines) {

        File file = new File(outputFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            for (String line : outputLines) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Output written to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing to output file: " + e.getMessage());
        }
    }

    /**
    * Sorts a map by value in descending order and returns a formatted string of the top N entries.
    * @param entries Map with class names as keys and counts as values
    * @param topN The maximum number of top entries to include
    * @return Formatted string of the top N entries
    */
    private static String sortMapByValueToString(Map<String, Integer> entries, int topN) {
        return entries.entrySet().stream()
                .sorted((e1, e2) -> {
                    // Primary sorting by value in descending order
                    int valueComparison = Integer.compare(e2.getValue(), e1.getValue());
                
                    // If values are the same, apply secondary sorting by key in alphabetical order
                    return valueComparison != 0 ? valueComparison : e1.getKey().compareTo(e2.getKey());
                })
                .limit(topN) // Limit to the top N entries
                .map(entry -> entry.getKey() + " (" + entry.getValue() + " occurrences)") // Format each entry
                .collect(Collectors.joining(", ")); // Join formatted entries into a single string
    }
}
