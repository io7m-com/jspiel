/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

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
    if (args.length != 1) {
      System.err.println("riff-chunks: file");
      System.exit(1);
    }

    final var path = Paths.get(args[0]);
    try (var channel = FileChannel.open(path, READ)) {
      final var size = channel.size();
      final var map = channel.map(READ_ONLY, 0L, size);
      final var parsers = new RiffParsers();
      final var parser = parsers.createForByteBuffer(path.toUri(), map);

      final var file = parser.parse();
      for (final var chunk : file.chunks()) {
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
          Long.valueOf(chunk.dataSizeIncludingForm().sizeUnpadded())),
        () -> System.out.printf(
          "CHUNK %s (size %d)\n",
          chunk.name().value(),
          Long.valueOf(chunk.dataSizeIncludingForm().sizeUnpadded())));

    for (final var subChunk : chunk.subChunks()) {
      showChunk(subChunk, depth + 1);
    }
  }
}
