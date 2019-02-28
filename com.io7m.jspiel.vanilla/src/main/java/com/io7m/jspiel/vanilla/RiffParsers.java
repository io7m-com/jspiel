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
import com.io7m.jspiel.api.RiffSize;
import com.io7m.jspiel.api.RiffSizes;
import org.osgi.service.component.annotations.Component;
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

@Component
public final class RiffParsers implements RiffFileParserProviderType
{
  private static final Logger LOG = LoggerFactory.getLogger(RiffParsers.class);

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
      final var starting_offset = Integer.toUnsignedLong(this.data.position());
      final var limit = this.data.remaining();

      if (LOG.isTraceEnabled()) {
        LOG.trace("starting parsing: position 0x{}, {} octet limit",
                  Long.toUnsignedString(starting_offset, 16),
                  Integer.valueOf(limit));
      }

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
            starting_offset);
        }
      }

      final var view = this.data.duplicate();
      view.position(Math.toIntExact(starting_offset));
      view.limit(limit);
      view.order(this.data.order());

      final var parse =
        new ChunkParser(0, Optional.empty(), this.source, view)
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
    private final RiffChunkID name;
    private final RiffSize size;
    private final List<RiffChunkType> sub_chunks;
    private final long offset;

    private RiffChunk(
      final Optional<RiffChunkType> in_parent,
      final long in_offset,
      final RiffChunkID in_name,
      final RiffSize in_size,
      final Optional<String> in_form_type,
      final List<RiffChunkType> in_sub_chunks)
    {
      this.parent =
        Objects.requireNonNull(in_parent, "parent");
      this.name =
        Objects.requireNonNull(in_name, "name");
      this.size =
        Objects.requireNonNull(in_size, "size");
      this.offset =
        in_offset;
      this.form_type =
        Objects.requireNonNull(in_form_type, "form_type");
      this.sub_chunks =
        Collections.unmodifiableList(Objects.requireNonNull(in_sub_chunks, "sub_chunks"));
    }

    @Override
    public String toString()
    {
      final var sb = new StringBuilder(128);

      sb.append("[RiffChunk ")
        .append(this.name.value());

      this.form_type.ifPresent(
        form_name -> sb.append("(")
          .append(form_name)
          .append(")"));

      sb.append(" offset 0x")
        .append(Long.toUnsignedString(this.offset, 16))
        .append(" size ")
        .append(this.size);

      if (!this.sub_chunks.isEmpty()) {
        sb.append(' ')
          .append(this.sub_chunks.size())
          .append(" subchunks");
      }

      sb.append(']');
      return sb.toString();
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
    public RiffSize dataSizeIncludingForm()
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
    private final ByteBuffer buffer;
    private final URI uri;
    private final byte[] buffer4;
    private final Optional<RiffChunkType> parent;
    private final int depth;

    ChunkParser(
      final int in_depth,
      final Optional<RiffChunkType> in_parent,
      final URI in_uri,
      final ByteBuffer in_data)
    {
      Preconditions.checkPreconditionL(
        in_data.remaining(),
        in_data.remaining() >= 0L,
        x -> "Limit must be non-zero");

      this.depth = in_depth;
      this.parent = Objects.requireNonNull(in_parent, "parent");
      this.uri = Objects.requireNonNull(in_uri, "uri");
      this.buffer = Objects.requireNonNull(in_data, "in_data");
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
      throws RiffParseException
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "[{}]: parsing subchunks ({} octet limit)",
          Integer.valueOf(this.depth),
          Long.valueOf(Integer.toUnsignedLong(this.buffer.limit())));
      }

      final var chunks = new ArrayList<RiffChunkType>(8);
      while (remaining(this.buffer) > 0L) {
        final var subchunk_start_offset =
          Integer.toUnsignedLong(this.buffer.position());

        final var name = this.readChunkName(this.buffer);
        final var size = this.readChunkSize(this.buffer, name);

        final var subchunk_data_offset =
          Integer.toUnsignedLong(this.buffer.position());

        this.checkSizeDoesNotExhaustRemaining(this.buffer, name, size.size());

        final var expected_subchunks_size = Math.toIntExact(size.size());
        final var view = this.buffer.slice();
        view.limit(expected_subchunks_size);
        view.order(this.buffer.order());

        switch (name.value()) {
          case FOURCC_LIST:
          case FOURCC_FFIR:
          case FOURCC_RIFX:
          case FOURCC_RIFF: {
            final var form_type =
              this.readFormType(view, name);

            final var sub_chunks = new ArrayList<RiffChunkType>(8);
            final var chunk =
              new RiffChunk(
                this.parent,
                this.absoluteOffsetFor(subchunk_start_offset),
                name,
                size,
                Optional.of(form_type),
                sub_chunks);

            if (LOG.isDebugEnabled()) {
              LOG.debug(
                "[{}]: chunk: 0x{} {} (form {}) (size {} [total {}])",
                Integer.valueOf(this.depth),
                Long.toUnsignedString(chunk.offset, 16),
                name.value(),
                form_type,
                size,
                Long.valueOf(chunk.totalSize()));
            }

            final var parser =
              new ChunkParser(this.depth + 1, Optional.of(chunk), this.uri, view);

            sub_chunks.addAll(parser.parse());
            chunks.add(chunk);

            /*
             * The received size is the sum of the sizes of all of the subchunks, plus four
             * octets for the form type at the start of this chunk.
             */

            final var sub_chunks_size = Math.addExact(sumSubchunks(sub_chunks), 4L);
            Postconditions.checkPostconditionL(
              sub_chunks_size,
              sub_chunks_size == expected_subchunks_size,
              received -> "Parsed subchunks size must match expected size " + expected_subchunks_size);
            break;
          }

          default:
            final var chunk =
              new RiffChunk(
                this.parent,
                this.absoluteOffsetFor(subchunk_start_offset),
                name,
                size,
                Optional.empty(),
                List.of());

            if (LOG.isDebugEnabled()) {
              LOG.debug(
                "[{}]: chunk: 0x{} {} (size {} [total {}])",
                Integer.valueOf(this.depth),
                Long.toUnsignedString(chunk.offset, 16),
                name.value(),
                size,
                Long.valueOf(chunk.totalSize()));
            }

            chunks.add(chunk);
            break;
        }

        final var seek_to = Math.addExact(subchunk_data_offset, size.size());
        this.buffer.position(Math.toIntExact(seek_to));
      }

      Postconditions.checkPostconditionL(
        remaining(this.buffer),
        remaining(this.buffer) == 0L,
        size -> "Remaining octets must be zero");

      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "[{}]: returning {} subchunks",
          Integer.valueOf(this.depth),
          Integer.valueOf(chunks.size()));
      }
      return chunks;
    }

    private long absoluteOffset()
    {
      return this.absoluteOffsetFor(Integer.toUnsignedLong(this.buffer.position()));
    }

    private long absoluteOffsetFor(final long relative)
    {
      return Math.addExact(this.parentDataOffset(), relative);
    }

    private long parentDataOffset()
    {
      return this.parent.map(p -> Long.valueOf(p.dataOffset()))
        .orElse(Long.valueOf(0L))
        .longValue();
    }

    private static long remaining(final ByteBuffer buffer)
    {
      return Integer.toUnsignedLong(buffer.remaining());
    }

    private void checkSizeDoesNotExhaustRemaining(
      final ByteBuffer view,
      final RiffChunkID name,
      final long size)
      throws RiffParseException
    {
      if (remaining(view) < size) {
        throw this.chunkSizeIllegal(name, this.absoluteOffset(), remaining(view), size);
      }
    }

    private RiffParseException chunkSizeIllegal(
      final RiffChunkID name,
      final long offset,
      final long remaining,
      final long size)
    {
      final var separator = System.lineSeparator();
      return new RiffParseException(
        new StringBuilder(128)
          .append("RIFF file specifies illegal chunk size")
          .append(separator)
          .append("  Problem: Chunk size exceeds the limit specified by the parent chunk")
          .append(separator)
          .append("  Chunk name: ")
          .append(name.value())
          .append(separator)
          .append("  Chunk offset: 0x")
          .append(Long.toUnsignedString(offset, 16))
          .append(separator)
          .append("  Remaining space: ")
          .append(Long.toUnsignedString(remaining))
          .append(separator)
          .append("  Specified size: ")
          .append(Long.toUnsignedString(size))
          .append(separator)
          .toString(),
        this.uri,
        offset);
    }

    private String readFormType(
      final ByteBuffer view,
      final RiffChunkID name)
      throws RiffParseException
    {
      this.checkRemainingBufferSpace(view, Optional.of(name), "Chunk form type", 4L);
      view.get(this.buffer4);
      // CHECKSTYLE:OFF
      return new String(this.buffer4, US_ASCII);
      // CHECKSTYLE:ON
    }

    private RiffSize readChunkSize(
      final ByteBuffer view,
      final RiffChunkID name)
      throws RiffParseException
    {
      this.checkRemainingBufferSpace(view, Optional.of(name), "Chunk size", 4L);
      final var size = Integer.toUnsignedLong(view.getInt());
      return RiffSizes.padIfNecessary(size);
    }

    private void checkRemainingBufferSpace(
      final ByteBuffer view,
      final Optional<RiffChunkID> name,
      final String reading,
      final long required)
      throws RiffParseException
    {
      if (required > remaining(view)) {
        final var separator = System.lineSeparator();
        final var message =
          new StringBuilder("Chunk data is truncated or does not match declared size.")
            .append(separator);

        name.ifPresent(
          n -> message.append("  Chunk name: ")
            .append(n.value())
            .append(separator));

        throw new RiffParseException(
          message
            .append("  Current offset: 0x")
            .append(Long.toUnsignedString(this.absoluteOffset()))
            .append(separator)
            .append("  Whilst reading: ")
            .append(reading)
            .append(separator)
            .append("  Required size: ")
            .append(Long.toUnsignedString(required, 10))
            .append(separator)
            .append("  Remaining size: ")
            .append(Long.toUnsignedString(remaining(view), 10))
            .append(separator)
            .toString(),
          this.uri,
          (long) this.buffer.position());
      }
    }

    private RiffChunkID readChunkName(
      final ByteBuffer view)
      throws RiffParseException
    {
      this.checkRemainingBufferSpace(view, Optional.empty(), "Chunk name", 4L);
      this.buffer.get(this.buffer4);
      return RiffChunkIDs.ofBytes(this.buffer4);
    }
  }
}
