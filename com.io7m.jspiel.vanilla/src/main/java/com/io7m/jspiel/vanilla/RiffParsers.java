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

package com.io7m.jspiel.vanilla;

import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jspiel.api.RiffChunkID;
import com.io7m.jspiel.api.RiffChunkIDs;
import com.io7m.jspiel.api.RiffChunkType;
import com.io7m.jspiel.api.RiffFileParserProviderType;
import com.io7m.jspiel.api.RiffFileParserType;
import com.io7m.jspiel.api.RiffFileType;
import com.io7m.jspiel.api.RiffParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * RIFF parsers.
 */

public final class RiffParsers implements RiffFileParserProviderType
{
  private static final String FOURCC_RIFF = "RIFF";
  private static final String FOURCC_FFIR = "FFIR";
  private static final String FOURCC_RIFX = "RIFX";
  private static final String FOURCC_LIST = "LIST";

  /**
   * Construct a RIFF parser provider.
   */

  public RiffParsers()
  {

  }

  @Override
  public RiffFileParserType createForByteBuffer(
    final URI source,
    final ByteBuffer data)
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(data, "data");
    return new RootParser(source, data);
  }

  private static final class RootParser implements RiffFileParserType
  {
    private final URI source;
    private final ByteBuffer data;

    RootParser(
      final URI in_source,
      final ByteBuffer in_data)
    {
      this.source = Objects.requireNonNull(in_source, "source");
      this.data = Objects.requireNonNull(in_data, "data");
    }

    @Override
    public RiffFileType parse()
      throws RiffParseException
    {
      final var offset = Integer.toUnsignedLong(this.data.position());
      final var limit = this.data.remaining();

      final var buffer4 = new byte[4];
      this.data.get(buffer4);

      // CHECKSTYLE:OFF
      final var name = new String(buffer4, US_ASCII);
      // CHECKSTYLE:ON

      switch (name) {
        case FOURCC_RIFF: {
          this.data.order(LITTLE_ENDIAN);
          break;
        }

        case FOURCC_FFIR: {
          this.data.order(BIG_ENDIAN);
          break;
        }

        case FOURCC_RIFX: {
          this.data.order(BIG_ENDIAN);
          break;
        }

        default: {
          final var separator = System.lineSeparator();
          throw new RiffParseException(
            new StringBuilder("Starting chunk must be RIFF")
              .append(separator)
              .append("  Expected: One of ")
              .append(String.join("|", List.of(FOURCC_RIFF, FOURCC_FFIR, FOURCC_RIFX)))
              .append(separator)
              .append("  Received: ")
              .append(name)
              .append(separator)
              .toString(),
            this.source,
            offset);
        }
      }

      this.data.position(Math.toIntExact(offset));

      final var parse =
        new ChunkParser(0, Optional.empty(), this.source, this.data, Integer.toUnsignedLong(limit))
          .parse();

      return new RiffFile(this.data.order(), parse);
    }

    private static final class RiffFile implements RiffFileType
    {
      private final ByteOrder order;
      private final List<RiffChunkType> chunks;

      RiffFile(
        final ByteOrder in_order,
        final List<RiffChunkType> in_chunks)
      {
        this.order = Objects.requireNonNull(in_order, "order");
        this.chunks = Objects.requireNonNull(in_chunks, "chunks");
      }

      @Override
      public List<RiffChunkType> chunks()
      {
        return this.chunks;
      }

      @Override
      public ByteOrder byteOrder()
      {
        return this.order;
      }
    }
  }

  private static final class RiffChunk implements RiffChunkType
  {
    private final Optional<String> form_type;
    private final Optional<RiffChunkType> parent;
    private RiffChunkID name;
    private long size;
    private List<RiffChunkType> sub_chunks;
    private long offset;

    private RiffChunk(
      final Optional<RiffChunkType> in_parent,
      final long in_offset,
      final RiffChunkID in_name,
      final long in_size,
      final Optional<String> in_form_type,
      final List<RiffChunkType> in_sub_chunks)
    {
      this.parent =
        Objects.requireNonNull(in_parent, "parent");
      this.name =
        Objects.requireNonNull(in_name, "name");
      this.offset =
        in_offset;
      this.size =
        in_size;
      this.form_type =
        Objects.requireNonNull(in_form_type, "form_type");
      this.sub_chunks =
        Collections.unmodifiableList(Objects.requireNonNull(in_sub_chunks, "sub_chunks"));
    }

    @Override
    public Optional<RiffChunkType> parent()
    {
      return this.parent;
    }

    @Override
    public RiffChunkID name()
    {
      return this.name;
    }

    @Override
    public long offset()
    {
      return this.offset;
    }

    @Override
    public long dataSize()
    {
      return this.size;
    }

    @Override
    public Optional<String> formType()
    {
      return this.form_type;
    }

    @Override
    public List<RiffChunkType> subChunks()
    {
      return this.sub_chunks;
    }
  }

  private static final class ChunkParser
  {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkParser.class);

    private final ByteBuffer data;
    private final long limit;
    private final URI uri;
    private final byte[] buffer4;
    private final Optional<RiffChunkType> parent;
    private final int depth;

    ChunkParser(
      final int in_depth,
      final Optional<RiffChunkType> in_parent,
      final URI in_uri,
      final ByteBuffer in_data,
      final long in_limit)
    {
      this.depth = in_depth;
      this.parent =
        Objects.requireNonNull(in_parent, "parent");
      this.uri =
        Objects.requireNonNull(in_uri, "uri");
      this.data =
        Objects.requireNonNull(in_data, "data");

      this.limit = in_limit;
      this.buffer4 = new byte[4];
    }

    private static long sumSubchunks(
      final Collection<RiffChunkType> sub_chunks)
    {
      return sub_chunks.stream()
        .mapToLong(RiffChunkType::totalSize)
        .reduce(0L, (x, y) -> x + y);
    }

    List<RiffChunkType> parse()
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "[{}]: parsing subchunks ({} octet limit)",
          Integer.valueOf(this.depth),
          Long.valueOf(this.limit));
      }

      final ArrayList<RiffChunkType> chunks = new ArrayList<>(8);

      final var position_start = Integer.toUnsignedLong(this.data.position());
      var remaining = this.limit;
      while (remaining > 0L) {
        this.checkBounds(position_start);

        final var offset = Integer.toUnsignedLong(this.data.position());
        final var name = this.readChunkName(position_start);
        final var size = this.readChunkSize(position_start);

        final var sub_chunks = new ArrayList<RiffChunkType>(8);
        switch (name.value()) {
          case FOURCC_FFIR:
          case FOURCC_RIFX:
          case FOURCC_RIFF: {
            final var form_type =
              this.readFormType(position_start);
            final var chunk =
              new RiffChunk(this.parent, offset, name, size, Optional.of(form_type), sub_chunks);

            if (LOG.isDebugEnabled()) {
              LOG.debug(
                "[{}]: chunk: 0x{} {} (form {}) (size {} [total {}])",
                Integer.valueOf(this.depth),
                Long.toUnsignedString(offset, 16),
                name.value(),
                form_type,
                Long.valueOf(size),
                Long.valueOf(chunk.totalSize()));
            }

            final var expected_sub_chunks_size = size - 4L;
            final var parser =
              new ChunkParser(
                this.depth + 1,
                Optional.of(chunk),
                this.uri,
                this.data,
                expected_sub_chunks_size);

            sub_chunks.addAll(parser.parse());
            chunks.add(chunk);

            final var sub_chunks_size = sumSubchunks(sub_chunks);
            Postconditions.checkPostconditionL(
              sub_chunks_size,
              sub_chunks_size == expected_sub_chunks_size,
              received -> "Subchunks size must match");
            break;
          }

          case FOURCC_LIST: {
            final var form_type =
              this.readFormType(position_start);
            final var chunk =
              new RiffChunk(this.parent, offset, name, size, Optional.of(form_type), sub_chunks);

            if (LOG.isDebugEnabled()) {
              LOG.debug(
                "[{}]: chunk: 0x{} {} (form {}) (size {} [total {}])",
                Integer.valueOf(this.depth),
                Long.toUnsignedString(offset, 16),
                name.value(),
                form_type,
                Long.valueOf(size),
                Long.valueOf(chunk.totalSize()));
            }

            final var expected_sub_chunks_size = size - 4L;
            final var parser =
              new ChunkParser(
                this.depth + 1,
                Optional.of(chunk),
                this.uri,
                this.data,
                expected_sub_chunks_size);

            sub_chunks.addAll(parser.parse());
            chunks.add(chunk);

            final var sub_chunks_size = sumSubchunks(sub_chunks);
            Postconditions.checkPostconditionL(
              sub_chunks_size,
              sub_chunks_size == expected_sub_chunks_size,
              received -> "Subchunks size must match");
            break;
          }

          default:
            final var chunk =
              new RiffChunk(this.parent, offset, name, size, Optional.empty(), List.of());

            if (LOG.isDebugEnabled()) {
              LOG.debug(
                "[{}]: chunk: 0x{} {} (size {} [total {}])",
                Integer.valueOf(this.depth),
                Long.toUnsignedString(offset, 16),
                name.value(),
                Long.valueOf(size),
                Long.valueOf(chunk.totalSize()));
            }

            chunks.add(chunk);
            break;
        }

        final long seek_size;
        final var element_size = 4L + 4L + size;
        if (size % 2L == 0L) {
          seek_size = element_size;
        } else {
          seek_size = element_size + 1L;
        }

        if (LOG.isTraceEnabled()) {
          LOG.trace(
            "[{}]: remaining: {} - {} = {}",
            Integer.valueOf(this.depth),
            Long.valueOf(remaining),
            Long.valueOf(seek_size),
            Long.valueOf(remaining - seek_size));
        }

        remaining -= seek_size;
        final var seek_to = offset + seek_size;
        this.data.position(Math.toIntExact(seek_to));
        this.checkBounds(position_start);
      }

      Postconditions.checkPostconditionL(
        remaining,
        remaining == 0L,
        size -> "Remaining octets must be zero");

      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "[{}]: returning {} subchunks",
          Integer.valueOf(this.depth),
          Integer.valueOf(chunks.size()));
      }
      return chunks;
    }

    private String readFormType(
      final long position_start)
    {
      this.data.get(this.buffer4);
      // CHECKSTYLE:OFF
      final var form_type = new String(this.buffer4, US_ASCII);
      // CHECKSTYLE:ON
      this.checkBounds(position_start);
      return form_type;
    }

    private long readChunkSize(
      final long position_start)
    {
      final var size = Integer.toUnsignedLong(this.data.getInt());
      this.checkBounds(position_start);
      return size;
    }

    private RiffChunkID readChunkName(
      final long position_start)
    {
      this.data.get(this.buffer4);
      this.checkBounds(position_start);
      return RiffChunkIDs.ofBytes(this.buffer4);
    }

    private void checkBounds(
      final long position_start)
    {
      Preconditions.checkPreconditionL(
        (long) this.data.position(),
        (long) this.data.position() <= position_start + this.limit,
        p -> "Position must be in bounds");
    }
  }
}
