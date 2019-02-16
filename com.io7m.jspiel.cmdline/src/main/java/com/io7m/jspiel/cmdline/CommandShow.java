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

package com.io7m.jspiel.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.jspiel.api.RiffChunkType;
import com.io7m.jspiel.api.RiffFileParserProviderType;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ServiceLoader;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.file.StandardOpenOption.READ;

@Parameters(commandDescription = "Display the contents of a RIFF file")
final class CommandShow extends CommandRoot
{
  // CHECKSTYLE:OFF

  @Parameter(
    names = "--file",
    required = true,
    description = "The RIFF file to display")
  Path path;

  // CHECKSTYLE:ON

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    final var parsers =
      ServiceLoader.load(RiffFileParserProviderType.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No RIFF file parser service available"));

    try (var channel = FileChannel.open(this.path, READ)) {
      final var map = channel.map(READ_ONLY, 0L, channel.size());
      final var parser = parsers.createForByteBuffer(this.path.toUri(), map);
      final var file = parser.parse();

      for (final var chunk : file.chunks()) {
        showChunk(chunk, 0);
      }
    }

    return null;
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
          "%s(%s) (size %d)\n",
          chunk.name().value(),
          formType,
          Long.valueOf(chunk.dataSizeExcludingForm().sizeUnpadded())),
        () -> System.out.printf(
          "%s (size %d)\n",
          chunk.name().value(),
          Long.valueOf(chunk.dataSizeExcludingForm().sizeUnpadded())));

    for (final var sub_chunk : chunk.subChunks()) {
      showChunk(sub_chunk, depth + 1);
    }
  }

  CommandShow()
  {

  }
}
