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
import com.io7m.jspiel.api.RiffFileWriterChunkDescriptionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import static java.nio.ByteOrder.BIG_ENDIAN;

public abstract class RiffFileBuildersContract
{
  protected abstract Logger logger();

  protected abstract RiffFileBuilderProviderType builders();

  @Test
  public final void testTrivial()
    throws Exception
  {
    final var logger = this.logger();
    final var builders = this.builders();
    final var builder = builders.create(BIG_ENDIAN);

    try (var root_chunk = builder.setRootChunk(RiffChunkID.of("RIFX"), "io7m")) {
      root_chunk.setSize(100L);
      root_chunk.setDataWriter(data -> { });
    }

    final var description = builder.build();
    Assertions.assertEquals(BIG_ENDIAN, description.byteOrder(), "Correct byte order");

    final var root = description.rootChunk();
    Assertions.assertEquals("RIFX", root.id().value(), "Correct chunk ID");
    Assertions.assertEquals(description, root.file(), "Correct file");
    Assertions.assertEquals(Optional.empty(), root.parent(), "Correct parent");
    Assertions.assertEquals(OptionalLong.of(100L), root.declaredSize(), "Correct size");
    Assertions.assertEquals(List.of(), root.subChunks(), "Correct child chunks");
  }

  @Test
  public final void testWriterNotChunks()
    throws Exception
  {
    final var logger = this.logger();
    final var builders = this.builders();
    final var builder = builders.create(BIG_ENDIAN);

    try (var root_chunk = builder.setRootChunk(RiffChunkID.of("RIFX"), "io7m")) {
      root_chunk.setSize(100L);
      root_chunk.setDataWriter(data -> { });

      Assertions.assertThrows(IllegalStateException.class, () -> {
        try (var x = root_chunk.addSubChunk(RiffChunkID.of("xxxx"))) {
          x.setDataWriter(data -> { });
        }
      });
    }
  }

  @Test
  public final void testChunksNotWriter()
    throws Exception
  {
    final var logger = this.logger();
    final var builders = this.builders();
    final var builder = builders.create(BIG_ENDIAN);

    try (var root_chunk = builder.setRootChunk(RiffChunkID.of("RIFX"), "io7m")) {
      try (var x = root_chunk.addSubChunk(RiffChunkID.of("xxxx"))) {
        x.setDataWriter(data -> { });
      }

      Assertions.assertThrows(IllegalStateException.class, () -> {
        root_chunk.setDataWriter(data -> { });
      });
    }
  }

  @Test
  public final void testSubchunks()
    throws Exception
  {
    final var logger = this.logger();
    final var builders = this.builders();
    final var builder = builders.create(BIG_ENDIAN);

    try (var root_chunk = builder.setRootChunk(RiffChunkID.of("RIFX"), "io7m")) {
      try (var list_chunk = root_chunk.addSubChunk(RiffChunkID.of("LIST"))) {
        list_chunk
          .setForm("m7oi")
          .setSize(100L);

        try (var ignored = list_chunk.addSubChunk(RiffChunkID.of("AAAA"))) {
          ignored.setDataWriter(data -> { });
        }
        try (var ignored = list_chunk.addSubChunk(RiffChunkID.of("BBBB"))) {
          ignored.setDataWriter(data -> { });
        }
        try (var ignored = list_chunk.addSubChunk(RiffChunkID.of("CCCC"))) {
          ignored.setDataWriter(data -> { });
        }
      }

      try (var list_chunk = root_chunk.addSubChunk(RiffChunkID.of("LIST"))) {
        list_chunk
          .setForm("oim7")
          .setSize(100L);

        try (var ignored = list_chunk.addSubChunk(RiffChunkID.of("DDDD"))) {
          ignored.setDataWriter(data -> { });
        }
        try (var ignored = list_chunk.addSubChunk(RiffChunkID.of("EEEE"))) {
          ignored.setDataWriter(data -> { });
        }
        try (var ignored = list_chunk.addSubChunk(RiffChunkID.of("FFFF"))) {
          ignored.setDataWriter(data -> { });
        }
      }
    }

    final var description = builder.build();
    logger.debug("{}", description);

    Assertions.assertEquals(BIG_ENDIAN, description.byteOrder(), "Correct byte order");

    final var root = description.rootChunk();
    logger.debug("{}", root);

    Assertions.assertEquals("RIFX", root.id().value(), "Correct chunk ID");
    Assertions.assertEquals("io7m", root.form().get(), "Correct form");
    Assertions.assertEquals(description, root.file(), "Correct file");
    Assertions.assertEquals(Optional.empty(), root.parent(), "Correct parent");
    Assertions.assertEquals(OptionalLong.empty(), root.declaredSize(), "Correct size");
    Assertions.assertEquals(2L, (long) root.subChunks().size(), "Correct child chunks");

    {
      final var list = root.subChunks().get(0);
      Assertions.assertEquals("LIST", list.id().value(), "Correct chunk ID");
      Assertions.assertEquals("m7oi", list.form().get(), "Correct form");
      Assertions.assertEquals(description, list.file(), "Correct file");
      Assertions.assertEquals(Optional.of(root), list.parent(), "Correct parent");
      Assertions.assertEquals(OptionalLong.of(100L), list.declaredSize(), "Correct size");
      Assertions.assertEquals(3L, (long) list.subChunks().size(), "Correct child chunks");

      {
        final var cc = list.subChunks().get(0);
        Assertions.assertEquals("AAAA", cc.id().value(), "Correct chunk ID");
        Assertions.assertEquals(Optional.empty(), cc.form(), "Correct form");
        Assertions.assertEquals(description, cc.file(), "Correct file");
        Assertions.assertEquals(Optional.of(list), cc.parent(), "Correct parent");
        Assertions.assertEquals(OptionalLong.empty(), cc.declaredSize(), "Correct size");
        Assertions.assertEquals(0L, (long) cc.subChunks().size(), "Correct child chunks");
      }

      {
        final var cc = list.subChunks().get(1);
        Assertions.assertEquals("BBBB", cc.id().value(), "Correct chunk ID");
        Assertions.assertEquals(Optional.empty(), cc.form(), "Correct form");
        Assertions.assertEquals(description, cc.file(), "Correct file");
        Assertions.assertEquals(Optional.of(list), cc.parent(), "Correct parent");
        Assertions.assertEquals(OptionalLong.empty(), cc.declaredSize(), "Correct size");
        Assertions.assertEquals(0L, (long) cc.subChunks().size(), "Correct child chunks");
      }

      {
        final var cc = list.subChunks().get(2);
        Assertions.assertEquals("CCCC", cc.id().value(), "Correct chunk ID");
        Assertions.assertEquals(Optional.empty(), cc.form(), "Correct form");
        Assertions.assertEquals(description, cc.file(), "Correct file");
        Assertions.assertEquals(Optional.of(list), cc.parent(), "Correct parent");
        Assertions.assertEquals(OptionalLong.empty(), cc.declaredSize(), "Correct size");
        Assertions.assertEquals(0L, (long) cc.subChunks().size(), "Correct child chunks");
      }
    }

    {
      final var list = root.subChunks().get(1);
      Assertions.assertEquals("LIST", list.id().value(), "Correct chunk ID");
      Assertions.assertEquals("oim7", list.form().get(), "Correct form");
      Assertions.assertEquals(description, list.file(), "Correct file");
      Assertions.assertEquals(Optional.of(root), list.parent(), "Correct parent");
      Assertions.assertEquals(OptionalLong.of(100L), list.declaredSize(), "Correct size");
      Assertions.assertEquals(3L, (long) list.subChunks().size(), "Correct child chunks");

      {
        final var cc = list.subChunks().get(0);
        Assertions.assertEquals("DDDD", cc.id().value(), "Correct chunk ID");
        Assertions.assertEquals(Optional.empty(), cc.form(), "Correct form");
        Assertions.assertEquals(description, cc.file(), "Correct file");
        Assertions.assertEquals(Optional.of(list), cc.parent(), "Correct parent");
        Assertions.assertEquals(OptionalLong.empty(), cc.declaredSize(), "Correct size");
        Assertions.assertEquals(0L, (long) cc.subChunks().size(), "Correct child chunks");
      }

      {
        final var cc = list.subChunks().get(1);
        Assertions.assertEquals("EEEE", cc.id().value(), "Correct chunk ID");
        Assertions.assertEquals(Optional.empty(), cc.form(), "Correct form");
        Assertions.assertEquals(description, cc.file(), "Correct file");
        Assertions.assertEquals(Optional.of(list), cc.parent(), "Correct parent");
        Assertions.assertEquals(OptionalLong.empty(), cc.declaredSize(), "Correct size");
        Assertions.assertEquals(0L, (long) cc.subChunks().size(), "Correct child chunks");
      }

      {
        final var cc = list.subChunks().get(2);
        Assertions.assertEquals("FFFF", cc.id().value(), "Correct chunk ID");
        Assertions.assertEquals(Optional.empty(), cc.form(), "Correct form");
        Assertions.assertEquals(description, cc.file(), "Correct file");
        Assertions.assertEquals(Optional.of(list), cc.parent(), "Correct parent");
        Assertions.assertEquals(OptionalLong.empty(), cc.declaredSize(), "Correct size");
        Assertions.assertEquals(0L, (long) cc.subChunks().size(), "Correct child chunks");
      }
    }

    final var linearized =
      description.linearizedChunks()
        .collect(Collectors.toList());

    Assertions.assertEquals(9, linearized.size(), "Linearized count correct");
    Assertions.assertEquals("RIFX", linearized.get(0).id().value(), "Correct ID");
    Assertions.assertEquals("LIST", linearized.get(1).id().value(), "Correct ID");
    Assertions.assertEquals("AAAA", linearized.get(2).id().value(), "Correct ID");
    Assertions.assertEquals("BBBB", linearized.get(3).id().value(), "Correct ID");
    Assertions.assertEquals("CCCC", linearized.get(4).id().value(), "Correct ID");
    Assertions.assertEquals("LIST", linearized.get(5).id().value(), "Correct ID");
    Assertions.assertEquals("DDDD", linearized.get(6).id().value(), "Correct ID");
    Assertions.assertEquals("EEEE", linearized.get(7).id().value(), "Correct ID");
    Assertions.assertEquals("FFFF", linearized.get(8).id().value(), "Correct ID");

    RiffFileWriterChunkDescriptionType prev = null;
    final var iter = linearized.iterator();
    while (iter.hasNext()) {
      final var curr = iter.next();

      if (prev != null) {
        Assertions.assertTrue(
          curr.ordinal() > prev.ordinal(), "Ordinal must increase");
      } else {
        Assertions.assertEquals(
          0L, curr.ordinal(), "Ordinal must start at zero");
      }

      prev = curr;
    }
  }
}
