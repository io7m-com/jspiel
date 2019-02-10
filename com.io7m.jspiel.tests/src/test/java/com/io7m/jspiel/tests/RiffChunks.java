package com.io7m.jspiel.tests;

import com.io7m.jspiel.api.RiffChunkType;
import com.io7m.jspiel.api.RiffParseException;
import com.io7m.jspiel.vanilla.RiffParsers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.file.StandardOpenOption.READ;

public final class RiffChunks
{
  private RiffChunks()
  {

  }

  public static void main(
    final String[] args)
    throws IOException, RiffParseException
  {
    final var path = Paths.get(args[0]);

    try (var channel = FileChannel.open(path, READ)) {
      final var size = channel.size();
      final var map = channel.map(READ_ONLY, 0L, size);

      final var parsers = new RiffParsers();
      final var parser = parsers.createForByteBuffer(path.toUri(), map);

      final var chunks = parser.parse();

      for (final var chunk : chunks) {
        showChunk(chunk, 0);
      }
    }
  }

  private static void showChunk(
    final RiffChunkType chunk,
    final int depth)
  {
    for (var i = 0; i < depth; ++i) {
      System.out.print("  ");
    }

    chunk.formType()
      .ifPresentOrElse(
        formType -> System.out.printf(
          "CHUNK %s (%s) (size %d)\n",
          chunk.name().value(),
          formType,
          Long.valueOf(chunk.dataSize())),
        () -> System.out.printf(
          "CHUNK %s (size %d)\n",
          chunk.name().value(),
          Long.valueOf(chunk.dataSize())));

    for (final var subChunk : chunk.subChunks()) {
      showChunk(subChunk, depth + 1);
    }
  }
}
