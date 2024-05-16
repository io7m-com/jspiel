jspiel
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.jspiel/com.io7m.jspiel.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.jspiel%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/com.io7m.jspiel/com.io7m.jspiel?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/jspiel/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m-com/jspiel.svg?style=flat-square)](https://codecov.io/gh/io7m-com/jspiel)
![Java Version](https://img.shields.io/badge/21-java?label=java&color=e6c35c)

![com.io7m.jspiel](./src/site/resources/jspiel.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/jspiel/main.linux.temurin.current.yml)](https://www.github.com/io7m-com/jspiel/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/jspiel/main.linux.temurin.lts.yml)](https://www.github.com/io7m-com/jspiel/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/jspiel/main.windows.temurin.current.yml)](https://www.github.com/io7m-com/jspiel/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/jspiel/main.windows.temurin.lts.yml)](https://www.github.com/io7m-com/jspiel/actions?query=workflow%3Amain.windows.temurin.lts)|

## jspiel

The `jspiel` package implements a set of types and functions for manipulating
RIFF files. It provides efficient parsing and serialization of RIFF files.

## Features

* Ultra-efficient memory-mapped RIFF parsing.
* Safe, easy, and correct lazy RIFF file output with chunk streaming.
* Command-line tools and API.
* Strongly-typed interfaces with a heavy emphasis on immutable value types.
* Fully documented (JavaDoc).
* Example code included.
* [OSGi-ready](https://www.osgi.org/)
* [JPMS-ready](https://en.wikipedia.org/wiki/Java_Platform_Module_System)
* ISC license.
* High-coverage automated test suite.

## Usage

### Reading

Memory-map a RIFF file and use a `RiffFileParserType` to obtain the RIFF
structure of the file:

```
final var parsers =
  ServiceLoader.load(RiffFileParserProviderType.class)
    .findFirst()
    .orElseThrow(() -> new IllegalStateException("No RIFF file parser service available"));

try (var channel = FileChannel.open(path, READ)) {
  final var map = channel.map(READ_ONLY, 0L, channel.size());
  final var parser = parsers.createForByteBuffer(this.path.toUri(), map);
  final var file = parser.parse();

  for (final var chunk : file.chunks()) {
    // Examine chunks and use the information to seek the channel
    // to different offsets within the chunks to obtain data.
    //
    // The chunks form a tree structure and have full parent/child
    // connectivity information.
  }
}
```

### Writing

Use a `RiffFileBuilderType` to express the RIFF structure of the intended file,
and then pass the built description to a `RiffFileWriterType` to serialize it.

```
final var builders =
  ServiceLoader.load(RiffFileBuilderProviderType.class)
    .findFirst()
    .orElseThrow(() -> new IllegalStateException("No RIFF file builder service available"));

final var writers =
  ServiceLoader.load(RiffFileWriterProviderType.class)
    .findFirst()
    .orElseThrow(() -> new IllegalStateException("No RIFF file writer service available"));

// Create a little-endian RIFF file description

final var builder =
  this.builders()
    .create(LITTLE_ENDIAN);

// The root chunk of the RIFF file must be RIFF, and has an "io7m" form
// in this case.

try (var root = builder.setRootChunk(RiffChunkID.of("RIFF"), "io7m")) {

  // The AAAA chunk is variable-length; the given writer function
  // can write as much data as it likes. The writer function is
  // presented with an NIO seekable byte channel interface where position 0
  // on the channel represents the start of the data section in the
  // chunk.

  try (var c = root.addSubChunk(RiffChunkID.of("AAAA"))) {
    c.setDataWriter(data -> data.write(...));
  }

  // The BBBB chunk is declared to have a length of 27 bytes; the given
  // writer function can write at most 27 bytes (and writing will be
  // terminated with an exception if it tries to write more). The library
  // will zero-pad any space that the writer does not use.
  //
  // The library also takes care of adding the padding
  // byte required by the RIFF specification (because 27 is not evenly
  // divisible into 16-bit words and so must be padded to 28 bytes).

  try (var c = root.addSubChunk(RiffChunkID.of("BBBB"))) {
    c.setSize(27L);
    c.setDataWriter(data -> data.write(...));
  }

  // The LIST chunk with form "cwss" contains three subchunks of varying
  // types and sizes.

  try (var c = root.addSubChunk(RiffChunkID.of("LIST"))) {
    c.setForm("cwss");

    try (var d = c.addSubChunk(RiffChunkID.of("cw05"))) {
      d.setSize(5L);
      d.setDataWriter(data -> data.write(...));
    }
    try (var d = c.addSubChunk(RiffChunkID.of("cw10"))) {
      d.setSize(10L);
      d.setDataWriter(data -> data.write(...));
    }
    try (var d = c.addSubChunk(RiffChunkID.of("cw20"))) {
      d.setSize(20L);
      d.setDataWriter(data -> data.write(...));
    }
  }
}

// Build the description and serialize it to the given byte channel.
// The writer functions specified in the description are called as the
// file is serialized.

final var description = builder.build();
try (final var channel = FileChannel.open(file, TRUNCATE_EXISTING, WRITE, CREATE)) {
  final var writer = writers.createForChannel(URI.create(file.toUri()), description, channel);
  writer.write();
}
```
