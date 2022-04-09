/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.jspiel.api.RiffChunkID;
import com.io7m.jspiel.api.RiffChunkType;
import com.io7m.jspiel.api.RiffFileParserProviderType;
import com.io7m.jspiel.api.RiffFileWriterDescriptionType;
import com.io7m.jspiel.api.RiffParseException;
import com.io7m.jspiel.api.RiffRequiredChunkMissingException;
import com.io7m.jspiel.api.RiffWriteException;
import com.io7m.jspiel.vanilla.RiffFileBuilders;
import com.io7m.jspiel.vanilla.RiffWriters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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

  protected abstract Logger logger();

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
    Assertions.assertEquals(0x0L, chunk.offset(), "Chunk offset matches");

    final var sc0 = chunk.subChunks().get(0);
    Assertions.assertEquals("fmt ", sc0.name().value(), "Chunk name matches");
    Assertions.assertEquals(18L, sc0.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(18L, sc0.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(0xcL, sc0.offset(), "Chunk offset matches");

    final var sc1 = chunk.subChunks().get(1);
    Assertions.assertEquals("fact", sc1.name().value(), "Chunk name matches");
    Assertions.assertEquals(4L, sc1.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(4L, sc1.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(0x26L, sc1.offset(), "Chunk offset matches");

    final var sc2 = chunk.subChunks().get(2);
    Assertions.assertEquals("data", sc2.name().value(), "Chunk name matches");
    Assertions.assertEquals(192000L, sc2.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(192000L, sc2.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(0x32L, sc2.offset(), "Chunk offset matches");
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
    Assertions.assertEquals(2L, (long) chunk.subChunks().size(), "Expected 2 subchunks");
    Assertions.assertEquals(0x0L, chunk.offset(), "Chunk offset matches");

    final var sc0 = chunk.subChunks().get(0);
    Assertions.assertEquals("fmt ", sc0.name().value(), "Chunk name matches");
    Assertions.assertEquals(16L, sc0.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(16L, sc0.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(0xcL, sc0.offset(), "Chunk offset matches");

    final var sc1 = chunk.subChunks().get(1);
    Assertions.assertEquals("data", sc1.name().value(), "Chunk name matches");
    Assertions.assertEquals(192000L, sc1.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(192000L, sc1.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(0x24L, sc1.offset(), "Chunk offset matches");
  }

  @Test
  public final void testComplex0()
    throws Exception
  {
    final var data = copyToByteBuffer("complex0.sf2");
    final var parsers = this.parsers();
    final var parser = parsers.createForByteBuffer(URI.create("complex0.sf2"), data);

    final var file = parser.parse();
    final var chunks = file.chunks();

    Assertions.assertEquals(LITTLE_ENDIAN, file.byteOrder(), "Correct byte order");

    Assertions.assertEquals(1L, (long) chunks.size(), "Expected one chunk");
    final var chunk = chunks.get(0);
    Assertions.assertEquals("RIFF", chunk.name().value(), "Chunk name matches");
    Assertions.assertEquals("sfbk", chunk.formType().get(), "Form type matches");
    Assertions.assertEquals(34076L, chunk.dataSizeIncludingForm().size(), "Size matches");
    Assertions.assertEquals(34072L, chunk.dataSizeExcludingForm().size(), "Size matches");
    Assertions.assertEquals(3L, (long) chunk.subChunks().size(), "Expected 3 subchunks");
    Assertions.assertEquals(0x0L, chunk.offset(), "Chunk offset matches");

    final var sc = chunk.subChunks();
    Assertions.assertEquals(3L, (long) sc.size(), "Expected 3 subchunks");

    {
      final var info = sc.get(0);
      Assertions.assertEquals("LIST", info.name().value(), "Correct name");
      Assertions.assertEquals("INFO", info.formType().get(), "Correct form");
      Assertions.assertEquals(0xCL, info.offset(), "Chunk offset matches");
      Assertions.assertEquals(154L, info.dataSizeExcludingForm().size(), "Size matches");

      {
        final var info_sc = info.subChunks().get(0);
        Assertions.assertEquals(0x18L, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("ifil", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(4L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var info_sc = info.subChunks().get(1);
        Assertions.assertEquals(0x24L, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("isng", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(8L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var info_sc = info.subChunks().get(2);
        Assertions.assertEquals(0x34L, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("INAM", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(10L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var info_sc = info.subChunks().get(3);
        Assertions.assertEquals(0x46L, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("IENG", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(12L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var info_sc = info.subChunks().get(4);
        Assertions.assertEquals(0x5AL, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("IPRD", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(20L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var info_sc = info.subChunks().get(5);
        Assertions.assertEquals(0x76L, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("ICOP", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(14L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var info_sc = info.subChunks().get(6);
        Assertions.assertEquals(0x8CL, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("ICMT", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(12L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var info_sc = info.subChunks().get(7);
        Assertions.assertEquals(0xA0L, info_sc.offset(), "Chunk offset matches");
        Assertions.assertEquals("ISFT", info_sc.name().value(), "Correct name");
        Assertions.assertEquals(10L, info_sc.dataSizeExcludingForm().size(), "Size matches");
      }
    }

    {
      final var sdta = sc.get(1);
      Assertions.assertEquals(0xB2L, sdta.offset(), "Chunk offset matches");
      Assertions.assertEquals("LIST", sdta.name().value(), "Correct name");
      Assertions.assertEquals("sdta", sdta.formType().get(), "Correct form");
      Assertions.assertEquals(33272L, sdta.dataSizeExcludingForm().size(), "Size matches");


      {
        final var scc = sdta.subChunks().get(0);
        Assertions.assertEquals(0xBEL, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("smpl", scc.name().value(), "Correct name");
        Assertions.assertEquals(33264L, scc.dataSizeExcludingForm().size(), "Size matches");
      }
    }

    {
      final var pdta = sc.get(2);
      Assertions.assertEquals(0x82B6L, pdta.offset(), "Chunk offset matches");
      Assertions.assertEquals("LIST", pdta.name().value(), "Correct name");
      Assertions.assertEquals("pdta", pdta.formType().get(), "Correct form");
      Assertions.assertEquals(610L, pdta.dataSizeExcludingForm().size(), "Size matches");

      {
        final var scc = pdta.subChunks().get(0);
        Assertions.assertEquals(0x82C2L, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("phdr", scc.name().value(), "Correct name");
        Assertions.assertEquals(152L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(1);
        Assertions.assertEquals(0x8362L, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("pbag", scc.name().value(), "Correct name");
        Assertions.assertEquals(28L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(2);
        Assertions.assertEquals(0x8386L, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("pmod", scc.name().value(), "Correct name");
        Assertions.assertEquals(10L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(3);
        Assertions.assertEquals(0x8398L, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("pgen", scc.name().value(), "Correct name");
        Assertions.assertEquals(28L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(4);
        Assertions.assertEquals(0x83bcL, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("inst", scc.name().value(), "Correct name");
        Assertions.assertEquals(88L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(5);
        Assertions.assertEquals(0x841CL, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("ibag", scc.name().value(), "Correct name");
        Assertions.assertEquals(32L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(6);
        Assertions.assertEquals(0x8444L, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("imod", scc.name().value(), "Correct name");
        Assertions.assertEquals(10L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(7);
        Assertions.assertEquals(0x8456L, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("igen", scc.name().value(), "Correct name");
        Assertions.assertEquals(52L, scc.dataSizeExcludingForm().size(), "Size matches");
      }

      {
        final var scc = pdta.subChunks().get(8);
        Assertions.assertEquals(0x8492L, scc.offset(), "Chunk offset matches");
        Assertions.assertEquals("shdr", scc.name().value(), "Correct name");
        Assertions.assertEquals(138L, scc.dataSizeExcludingForm().size(), "Size matches");
      }
    }
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
      ignored.setDataWriter(data -> { });
    }

    final var ex = Assertions.assertThrows(
      RiffParseException.class,
      () -> serializeThenParseRIFF(parsers, writers, builder.build()));

    this.logger().debug("exception: ", ex);
    Assertions.assertTrue(ex.getMessage().contains("truncated or does not match declared size"));
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

    this.logger().debug("exception: ", ex);
    Assertions.assertTrue(ex.getMessage().contains("truncated or does not match declared size"));
  }

  /**
   * Try various corrupted RIFF files. Essentially, this checks that corrupted files never cause
   * the parser to throw unchecked exceptions (indicating some sort of internal invariant
   * violation).
   *
   * @return A list of tests
   */

  @TestFactory
  public final List<DynamicTest> testCorruption()
  {
    return LongStream.range(0L, 10_000L)
      .mapToObj(seed -> {
        final var name = "testCorruptionWithSeed" + seed;
        return DynamicTest.dynamicTest(name, () -> {
          final var logger = this.logger();
          try {
            final var map = copyToByteBuffer("complex0.sf2");
            final var corrupted_map = corruptMap(logger, map, seed);
            final var parsers = this.parsers();
            final var parser = parsers.createForByteBuffer(URI.create(name), corrupted_map);
            parser.parse();
          } catch (RiffParseException e) {
            logger.error("parse exception: ", e);
          } catch (RuntimeException e) {
            Assertions.fail(e);
          }
        });
      })
      .collect(Collectors.toList());
  }

  private static ByteBuffer corruptMap(
    final Logger logger,
    final ByteBuffer map,
    final long seed)
  {
    final var corrupted_map = ByteBuffer.allocate(map.capacity());
    corrupted_map.put(map);
    corrupted_map.position(0);

    final var rng = new Random(seed);
    final var corruption = rng.nextDouble() * 0.01;

    logger.debug(
      "seed {}: corrupting {}% of the input bytes",
      Long.valueOf(seed),
      String.format("%.2f", Double.valueOf(corruption * 100.0)));

    final var bytes = new byte[1];
    for (var index = 0; index < corrupted_map.capacity(); ++index) {
      if (rng.nextDouble() < corruption) {
        rng.nextBytes(bytes);
        corrupted_map.put(index, bytes[0]);
      }
    }
    return corrupted_map;
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
