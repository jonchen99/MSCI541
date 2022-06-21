# msci-541-f21-hw4-jonchen99
msci-541-f21-hw4-jonchen99 created by GitHub Classroom

HW #4
Jonathan Chen
20722167

This repository consists of five main programs: IndexEngine.java, GetDoc.java, BooleanAND.java, Evaluate.java, and BM25.java. These five files are contained within the file path `src/com/jonathan`

These programs were built using Java (version 17). 
```
java version "17" 2021-09-14 LTS
Java(TM) SE Runtime Environment (build 17+35-LTS-2724)
Java HotSpot(TM) 64-Bit Server VM (build 17+35-LTS-2724, mixed mode, sharing)
```

# Topics File
For this assignment, the topics file is named `queries.txt` and it is available in the root of the repository. 

# Running the code
To run the code from the command line, use the prebuilt jar files available under `out/artifacts/`.

### Running IndexEngine
1. Go to the `out/artifacts/IndexEngine_jar` directory in the command line
2. Run 
```
java -jar IndexEngine.jar [path_to_latimes.gz] [output_path] [use of porter stemmer ('True'/'False')]
```

### Running GetDoc
1. Go to the `out/artifacts/GetDoc_jar` directory in the command line
2. Run 
```
java -jar GetDoc.jar [path to indexed data] ['id' or 'docno'] [id or docno value]
```

### Running BooleanAND
1. Go to the `out/artifacts/BooleanAND_jar` directory in the command line
2. Run 
```
java -jar BooleanAND.jar [path to indexed data] [path to queries file] [output filename]
```

### Running Evaluate
1. Go to the `out/artifacts/Evaluate_jar` directory in the command line
2. Run 
```
java -jar Evaluate.jar [path to indexed data] [path to qrels file] [path to results file] [output directory] [output csv filename]
```

### Running BM25
1. Go to the `out/artifacts/BM25_jar` directory in the command line
2. Run 
```
java -jar BM25.jar [path to indexed data] [path to queries file] [use of porter stemmer ('True'/'False')] [output directory]
```
