# Java Reflection Metadata Analyzer

A tool designed to inspect Java class hierarchies using the **Java Reflection API**. This project scans type systems to identify and rank classes based on structural metrics such as field counts, method signatures, and inheritance depth.

---

## Technical Overview

* **Type Inspection**: Traverses class hierarchies to calculate declared and inherited members.
* **Module System Awareness**: Loads and filters types from the Java 21 boot layer, specifically targeting `java.*` and `jdk.*` packages.
* **Metric-Based Ranking**: Ranks the "Top N" classes based on fields, unique method signatures, and subtype/supertype counts.

---

## Features

* **Hierarchy Traversal**: Analyzes fields and methods while respecting access modifiers, such as filtering private members from superclasses.
* **Uniqueness Checking**: Uses `Java Sets` to manage same-named methods or fields resulting from overloading, overriding, or shadowing.
* **Filtering**: Skips special entries like `package-info`, `module-info`, and `META-INF` to focus on functional classes.
* **Flexible Data Input**: Supports scanning the entire JDK or analyzing targeted libraries via input files.

---

## Usage

### 1. JDK Analysis

Analyze the top 10 classes within the standard Java libraries:

```bash
java Main 10

```

### 2. Targeted Analysis

Analyze specific classes from an input file and save results to an output file:

```bash
java -cp .;libraries\commons-lang3-3.17.0.jar Main resources\input.txt resources\output.txt 10

```

---

## Documentation

The API documentation, generated via Javadoc, is available here: **[Javadoc Page](https://evankost.github.io/reflection-analyzer-java/docs/index.html)**

---

## Building and Running

The project is structured for command-line execution. Ensure you are in the root directory before running these commands.

**1. Compile**

```bash
javac -d bin -cp "libraries/*" ClassScanner.java Main.java

```

**2. Run (JDK Mode)**

```bash
java -cp "bin;libraries/*" Main 10

```

**3. Run (Target File Mode)**

```bash
java -cp "bin;libraries/*" Main resources/input.txt resources/output.txt 10

```

---

## License

This project is licensed under the MIT License.
