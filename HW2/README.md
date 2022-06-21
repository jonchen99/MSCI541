# msci-541-f21-hw2-jonchen99
msci-541-f21-hw2-jonchen99 created by GitHub Classroom

HW #2
Jonathan Chen
20722167

This repository consists of three main programs: IndexEngine.java, GetDoc.java, and BooleanAND.java. These three files are contained within the file path `src/com/jonathan`

These programs were built using Java (version 17). 
```
java version "17" 2021-09-14 LTS
Java(TM) SE Runtime Environment (build 17+35-LTS-2724)
Java HotSpot(TM) 64-Bit Server VM (build 17+35-LTS-2724, mixed mode, sharing)
```

# Running the code
To run the code from the command line, use the prebuilt jar files available under `out/artifacts/`.

### Running IndexEngine
1. Go to the `out/artifacts/IndexEngine_jar` directory in the command line
2. Run 
```
java -jar IndexEngine.jar [path_to_latimes.gz] [output_path]
```

### Running GetDoc
1. Go to the `out/artifacts/GetDoc_jar` directory in the command line
2. Run 
```
java -jar GetDoc.jar [path to indexed data] ['id' or 'docno'] [id or docno value]
```

### Running BooleanAND
1. Go to the `out/artifacts/BooleanAND` directory in the command line
2. Run 
```
java -jar BooleanAND.jar [path to indexed data] [path to queries file] [output filename]
```


