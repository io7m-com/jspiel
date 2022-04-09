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
import com.io7m.jspiel.api.RiffFileBuilderProviderType;
import com.io7m.jspiel.api.RiffFileParserProviderType;
import com.io7m.jspiel.api.RiffFileWriterProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public abstract class RiffWritersContract
{
  private static ByteBuffer countingBuffer(
    final int i)
  {
    final var buffer = ByteBuffer.allocate(i);
    for (var index = 0; index < buffer.remaining(); ++index) {
      buffer.put(index, (byte) index);
    }
    return buffer;
  }

  protected abstract Logger logger();

  protected abstract RiffFileParserProviderType parsers();

  protected abstract RiffFileWriterProviderType writers();

  protected abstract RiffFileBuilderProviderType builders();

  @Test
  public void testSimpleStructure()
    throws Exception
  {
    final var logger = this.logger();

    final var temp = Files.createTempFile("riffwriter-test-", ".riff");
    logger.debug("temp: {}", temp);

    final var builder =
      this.builders()
        .create(LITTLE_ENDIAN);

    try (var root = builder.setRootChunk(RiffChunkID.of("RIFF"), "io7m")) {
      root.setSize(15L);

      try (var c = root.addSubChunk(RiffChunkID.of("AAAA"))) {
        c.setDataWriter(data -> data.write(countingBuffer(13)));
      }
      try (var c = root.addSubChunk(RiffChunkID.of("BBBB"))) {
        c.setSize(27L);
        c.setDataWriter(data -> data.write(countingBuffer(27)));
      }
      try (var c = root.addSubChunk(RiffChunkID.of("LIST"))) {
        c.setForm("cwss");

        try (var d = c.addSubChunk(RiffChunkID.of("cw05"))) {
          d.setSize(5L);
          d.setDataWriter(data -> data.write(countingBuffer(5)));
        }
        try (var d = c.addSubChunk(RiffChunkID.of("cw10"))) {
          d.setSize(10L);
          d.setDataWriter(data -> data.write(countingBuffer(10)));
        }
        try (var d = c.addSubChunk(RiffChunkID.of("cw20"))) {
          d.setSize(20L);
          d.setDataWriter(data -> data.write(countingBuffer(10)));
        }
      }
    }

    final var description = builder.build();
    try (final var channel = FileChannel.open(temp, TRUNCATE_EXISTING, WRITE, CREATE)) {
      final var writers = this.writers();

      final var writer =
        writers.createForChannel(URI.create("urn:file"), description, channel);

      writer.write();
      channel.force(true);
    }

    try (final var channel = FileChannel.open(temp, READ)) {
      final var parsers = this.parsers();
      final var map = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
      final var parser = parsers.createForByteBuffer(URI.create("urn:file"), map);
      final var file = parser.parse();

      Assertions.assertEquals(LITTLE_ENDIAN, file.byteOrder(), "Correct byte order");
      final var chunks = file.linearizedDescendantChunks().collect(Collectors.toList());
      Assertions.assertEquals(7, chunks.size(), "Correct chunk count");

      chunks.forEach(chunk -> logger.debug("chunk {}", chunk));

      Assertions.assertEquals("RIFF", chunks.get(0).name().value(), "Correct name");
      Assertions.assertEquals("AAAA", chunks.get(1).name().value(), "Correct name");
      Assertions.assertEquals("BBBB", chunks.get(2).name().value(), "Correct name");
      Assertions.assertEquals("LIST", chunks.get(3).name().value(), "Correct name");
      Assertions.assertEquals("cw05", chunks.get(4).name().value(), "Correct name");
      Assertions.assertEquals("cw10", chunks.get(5).name().value(), "Correct name");
      Assertions.assertEquals("cw20", chunks.get(6).name().value(), "Correct name");

      Assertions.assertEquals(134L, chunks.get(0).dataSizeIncludingForm().size(), "Correct size");
      Assertions.assertEquals(14L, chunks.get(1).dataSizeIncludingForm().size(), "Correct size");
      Assertions.assertEquals(28L, chunks.get(2).dataSizeIncludingForm().size(), "Correct size");
      Assertions.assertEquals(64L, chunks.get(3).dataSizeIncludingForm().size(), "Correct size");
      Assertions.assertEquals(6L, chunks.get(4).dataSizeIncludingForm().size(), "Correct size");
      Assertions.assertEquals(10L, chunks.get(5).dataSizeIncludingForm().size(), "Correct size");
      Assertions.assertEquals(20L, chunks.get(6).dataSizeIncludingForm().size(), "Correct size");

      Assertions.assertEquals(3L, chunks.get(0).subChunks().size(), "Correct subchunks");
      Assertions.assertEquals(0L, chunks.get(1).subChunks().size(), "Correct subchunks");
      Assertions.assertEquals(0L, chunks.get(2).subChunks().size(), "Correct subchunks");
      Assertions.assertEquals(3L, chunks.get(3).subChunks().size(), "Correct subchunks");
      Assertions.assertEquals(0L, chunks.get(4).subChunks().size(), "Correct subchunks");
      Assertions.assertEquals(0L, chunks.get(5).subChunks().size(), "Correct subchunks");
      Assertions.assertEquals(0L, chunks.get(6).subChunks().size(), "Correct subchunks");
    }
  }
}
