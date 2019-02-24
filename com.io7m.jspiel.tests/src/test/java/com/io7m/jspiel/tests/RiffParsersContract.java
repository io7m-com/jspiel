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

import com.io7m.jspiel.api.RiffBuilderException;
import com.io7m.jspiel.api.RiffChunkID;
import com.io7m.jspiel.api.RiffFileParserProviderType;
import com.io7m.jspiel.api.RiffFileWriterDescriptionType;
import com.io7m.jspiel.api.RiffParseException;
import com.io7m.jspiel.api.RiffRequiredChunkMissingException;
import com.io7m.jspiel.api.RiffWriteException;
import com.io7m.jspiel.vanilla.RiffFileBuilders;
import com.io7m.jspiel.vanilla.RiffWriters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public abstract class RiffParsersContract
{
  private static ByteBuffer copyToByteBuffer(
    final String name)
    throws IOException
  {
    final var path = "/com/io7m/jspiel/tests/" + name;
    try (var stream = RiffParsersContract.class.getResourceAsStream(path)) {
      try (var output = new ByteArrayOutputStream(1024)) {
        stream.transferTo(output);
        return ByteBuffer.wrap(output.toByteArray());
      }
    }
  }

  protected abstract RiffFileParserProviderType parsers();

  @Test
  public final void test000_12_be()
    throws Exception
  {
    final var data = copyToByteBuffer("000_12_be.wav");
    final var parsers = this.parsers();
    final var parser = parsers.createForByteBuffer(URI.create("000_12_be.wav"), data);

    final var file = parser.parse();
    final var chunks = file.chunks();

    Assertions.assertEquals(BIG_ENDIAN, file.byteOrder(), "Correct byte order");

    Assertions.assertEquals(1L, (long) chunks.size(), "Expected one chunk");
    final var chunk = chunks.get(0);
    Assertions.assertEquals("RIFX", chunk.name().value(), "Chunk name matches");
    Assertions.assertEquals("WAVE", chunk.formType().get(), "Form type matches");
    Assertions.assertEquals(192050L, chunk.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(192046L, chunk.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(3L, (long) chunk.subChunks().size(), "Expected 3 subchunks");

    final var sc0 = chunk.subChunks().get(0);
    Assertions.assertEquals("fmt ", sc0.name().value(), "Chunk name matches");
    Assertions.assertEquals(18L, sc0.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(18L, sc0.dataSizeExcludingForm().size(), "Size matches");

    final var sc1 = chunk.subChunks().get(1);
    Assertions.assertEquals("fact", sc1.name().value(), "Chunk name matches");
    Assertions.assertEquals(4L, sc1.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(4L, sc1.dataSizeExcludingForm().size(), "Size matches");

    final var sc2 = chunk.subChunks().get(2);
    Assertions.assertEquals("data", sc2.name().value(), "Chunk name matches");
    Assertions.assertEquals(192000L, sc2.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(192000L, sc2.dataSizeExcludingForm().size(), "Size matches");
  }

  @Test
  public final void test000_12_le()
    throws Exception
  {
    final var data = copyToByteBuffer("000_12_le.wav");
    final var parsers = this.parsers();
    final var parser = parsers.createForByteBuffer(URI.create("000_12_le.wav"), data);

    final var file = parser.parse();
    final var chunks = file.chunks();

    Assertions.assertEquals(LITTLE_ENDIAN, file.byteOrder(), "Correct byte order");

    Assertions.assertEquals(1L, (long) chunks.size(), "Expected one chunk");
    final var chunk = chunks.get(0);
    Assertions.assertEquals("RIFF", chunk.name().value(), "Chunk name matches");
    Assertions.assertEquals("WAVE", chunk.formType().get(), "Form type matches");
    Assertions.assertEquals(192036L, chunk.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(192032L, chunk.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(2L, (long) chunk.subChunks().size(), "Expected 3 subchunks");

    final var sc0 = chunk.subChunks().get(0);
    Assertions.assertEquals("fmt ", sc0.name().value(), "Chunk name matches");
    Assertions.assertEquals(16L, sc0.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(16L, sc0.dataSizeExcludingForm().size(), "Size matches");

    final var sc1 = chunk.subChunks().get(1);
    Assertions.assertEquals("data", sc1.name().value(), "Chunk name matches");
    Assertions.assertEquals(192000L, sc1.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(192000L, sc1.dataSizeExcludingForm().size(), "Size matches");
  }

  @Test
  public final void testFinds()
    throws Exception
  {
    final var data = copyToByteBuffer("000_12_be.wav");
    final var parsers = this.parsers();
    final var parser = parsers.createForByteBuffer(URI.create("000_12_be.wav"), data);

    final var file = parser.parse();
    final var chunks = file.chunks();

    Assertions.assertEquals(BIG_ENDIAN, file.byteOrder(), "Correct byte order");

    Assertions.assertEquals(1L, (long) chunks.size(), "Expected one chunk");
    final var chunk = chunks.get(0);

    Assertions.assertThrows(
      RiffRequiredChunkMissingException.class,
      () -> chunk.findRequiredSubChunks(RiffChunkID.of("none")));
    Assertions.assertThrows(
      RiffRequiredChunkMissingException.class,
      () -> chunk.findRequiredSubChunks("none"));
    Assertions.assertThrows(
      RiffRequiredChunkMissingException.class,
      () -> chunk.findRequiredSubChunk(RiffChunkID.of("none")));
    Assertions.assertThrows(
      RiffRequiredChunkMissingException.class,
      () -> chunk.findRequiredSubChunk("none"));

    Assertions.assertEquals(
      "fmt ",
      chunk.findRequiredSubChunk("fmt ").name().value());
    Assertions.assertEquals(
      "fmt ",
      chunk.findRequiredSubChunk(RiffChunkID.of("fmt ")).name().value());
    Assertions.assertEquals(
      "fmt ",
      chunk.findRequiredSubChunks("fmt ").get(0).name().value());
    Assertions.assertEquals(
      "fmt ",
      chunk.findRequiredSubChunks(RiffChunkID.of("fmt ")).get(0).name().value());

    Assertions.assertEquals(
      "fmt ",
      chunk.findOptionalSubChunk("fmt ").get().name().value());
    Assertions.assertEquals(
      "fmt ",
      chunk.findOptionalSubChunk(RiffChunkID.of("fmt ")).get().name().value());
    Assertions.assertEquals(
      "fmt ",
      chunk.findOptionalSubChunks("fmt ").collect(Collectors.toList()).get(0).name().value());
    Assertions.assertEquals(
      "fmt ",
      chunk.findOptionalSubChunks(RiffChunkID.of("fmt ")).collect(Collectors.toList()).get(0).name().value());

    Assertions.assertEquals(
      Optional.empty(),
      chunk.findOptionalSubChunk("none"));
    Assertions.assertEquals(
      Optional.empty(),
      chunk.findOptionalSubChunk(RiffChunkID.of("none")));
    Assertions.assertEquals(
      List.of(),
      chunk.findOptionalSubChunks("none").collect(Collectors.toList()));
    Assertions.assertEquals(
      List.of(),
      chunk.findOptionalSubChunks(RiffChunkID.of("none")).collect(Collectors.toList()));
  }

  @Test
  public final void testTooSmall0()
  {
    final var parsers = this.parsers();
    final var writers = new RiffWriters();

    final var builders = new RiffFileBuilders();
    final var builder = builders.create(LITTLE_ENDIAN);

    try (var ignored = builder.setRootChunk(RiffChunkID.of("RIFF"), "badx")) {

    }

    final var ex = Assertions.assertThrows(
      RiffParseException.class,
      () -> serializeThenParseRIFF(parsers, writers, builder.build()));

    Assertions.assertTrue(ex.getMessage().contains("too small"));
  }

  @Test
  public final void testTooSmall1()
    throws Exception
  {
    final var parsers = this.parsers();
    final var writers = new RiffWriters();

    final var builders = new RiffFileBuilders();
    final var builder = builders.create(LITTLE_ENDIAN);

    try (var chunk = builder.setRootChunk(RiffChunkID.of("RIFF"), "badx")) {
      chunk.addSubChunk(RiffChunkID.of("abcd"));
    }

    serializeThenParseRIFF(parsers, writers, builder.build());
  }

  @Test
  public final void testTooSmall2()
  {
    final var parsers = this.parsers();
    final var writers = new RiffWriters();

    final var builders = new RiffFileBuilders();
    final var builder = builders.create(LITTLE_ENDIAN);

    try (var chunk = builder.setRootChunk(RiffChunkID.of("RIFF"), "badx")) {
      chunk.setSize(8L);
      chunk.setDataWriter(data -> ByteBuffer.allocate(8));
    }

    final var ex = Assertions.assertThrows(
      RiffParseException.class,
      () -> serializeThenParseRIFF(parsers, writers, builder.build()));

    Assertions.assertTrue(ex.getMessage().contains("too small to contain any subchunks"));
  }

  private static void serializeThenParseRIFF(
    final RiffFileParserProviderType parsers,
    final RiffWriters writers,
    final RiffFileWriterDescriptionType built)
    throws IOException, RiffWriteException, RiffParseException
  {
    final var path = Files.createTempFile("jspiel-", ".riff");
    try (final var channel = FileChannel.open(path, READ, WRITE, CREATE, TRUNCATE_EXISTING)) {
      final var writer = writers.createForChannel(path.toUri(), built, channel);
      writer.write();

      final var map = channel.map(READ_ONLY, 0L, channel.size());
      final var parser = parsers.createForByteBuffer(path.toUri(), map);
      parser.parse();
    }
  }
}
